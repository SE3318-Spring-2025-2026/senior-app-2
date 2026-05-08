package com.seniorapp.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.seniorapp.dto.ai.PrReviewDtos;
import com.seniorapp.exception.AiValidationTimeoutException;
import com.seniorapp.service.SecureOutboundApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.*;

/**
 * Level 3 AI — Pull-Request Review Validator.
 *
 * <p>Pipeline:
 * <ol>
 *   <li>Fetch PR review comments from GitHub via {@link SecureOutboundApiService}.</li>
 *   <li>Send the comments to OpenAI with a structured prompt.</li>
 *   <li>Parse the structured JSON response: {@code review_score}, {@code is_meaningful}, {@code summary}.</li>
 * </ol>
 *
 * <p>Scoring contract:
 * <ul>
 *   <li>Superficial comments ("LGTM", "+1", "Looks good") → {@code is_meaningful=false}, score ≤ 25.</li>
 *   <li>Architectural / design discussions → {@code is_meaningful=true}, score ≥ 70.</li>
 * </ul>
 *
 * <p>All outbound HTTP calls use {@link SecureOutboundApiService} — tokens are never logged.
 */
@Service
public class AiPullRequestReviewValidatorService {

    private static final Logger log = LoggerFactory.getLogger(AiPullRequestReviewValidatorService.class);

    private static final String GITHUB_API_BASE = "https://api.github.com";
    private static final String OPENAI_CHAT_ENDPOINT = "https://api.openai.com/v1/chat/completions";

    private final SecureOutboundApiService secureOutboundApiService;
    private final ObjectMapper objectMapper;
    private final String openAiApiKey;
    private final String openAiModel;
    private final long timeoutMs;

    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "ai-pr-review-" + System.nanoTime());
        t.setDaemon(true);
        return t;
    });

    public AiPullRequestReviewValidatorService(
            SecureOutboundApiService secureOutboundApiService,
            ObjectMapper objectMapper,
            @Value("${openai.api-key:}") String openAiApiKey,
            @Value("${openai.model:gpt-4o-mini}") String openAiModel,
            @Value("${ai.request-timeout-ms:30000}") long timeoutMs) {
        this.secureOutboundApiService = secureOutboundApiService;
        this.objectMapper = objectMapper;
        this.openAiApiKey = openAiApiKey;
        this.openAiModel = openAiModel;
        this.timeoutMs = timeoutMs;
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Validates whether meaningful software-engineering review occurred on a PR.
     *
     * @param encryptedGithubToken encrypted GitHub PAT
     * @param repoOwner            GitHub organisation / owner
     * @param repoName             repository name
     * @param prNumber             pull-request number
     * @return structured validation result
     * @throws AiValidationTimeoutException if OpenAI does not respond within the timeout
     */
    public PrReviewDtos.PrReviewResult validatePrReview(String encryptedGithubToken,
                                                         String repoOwner,
                                                         String repoName,
                                                         int prNumber) {
        List<String> comments = fetchPrReviewComments(encryptedGithubToken, repoOwner, repoName, prNumber);

        if (comments.isEmpty()) {
            log.info("[AiPrReview] PR {}/{}/{} has no review comments – returning zero score.", repoOwner, repoName, prNumber);
            return new PrReviewDtos.PrReviewResult(0, false,
                    "No review comments were found on this pull request.", "No comments to evaluate.");
        }

        if (openAiApiKey == null || openAiApiKey.isBlank()) {
            log.warn("[AiPrReview] OpenAI API key not configured – returning fallback result.");
            return new PrReviewDtos.PrReviewResult(0, false,
                    "AI review unavailable: OpenAI API key is not configured.", "No key.");
        }

        Callable<PrReviewDtos.PrReviewResult> task = () -> callOpenAiForReview(comments);
        Future<PrReviewDtos.PrReviewResult> future = executor.submit(task);
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new AiValidationTimeoutException(
                    "PR review AI validation exceeded timeout of " + timeoutMs + "ms", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof AiValidationTimeoutException t) throw t;
            throw new RuntimeException("PR review AI validation failed: " + cause.getMessage(), cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("PR review AI validation interrupted", e);
        }
    }

    // ── GitHub PR Review Comments ──────────────────────────────────────────────

    /**
     * Fetches review comments for a GitHub PR.
     * Returns a list of comment body strings (may be empty).
     *
     * @param encryptedToken encrypted GitHub PAT
     */
    List<String> fetchPrReviewComments(String encryptedToken,
                                        String repoOwner,
                                        String repoName,
                                        int prNumber) {
        // /repos/{owner}/{repo}/pulls/{pull_number}/comments  – line-level review comments
        String url = GITHUB_API_BASE + "/repos/" + repoOwner + "/" + repoName
                + "/pulls/" + prNumber + "/comments";

        ResponseEntity<String> response = secureOutboundApiService
                .executeGitHubApiCall(encryptedToken, url, null, HttpMethod.GET);

        if (response == null || !response.getStatusCode().is2xxSuccessful()) {
            int code = response != null ? response.getStatusCode().value() : -1;
            log.warn("[AiPrReview] GitHub returned non-2xx {} fetching PR comments for {}/{}/{}", code, repoOwner, repoName, prNumber);
            if (code == 429) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.TOO_MANY_REQUESTS,
                        "GitHub rate limit exceeded while fetching PR comments");
            }
            return List.of();
        }

        return parseCommentBodies(response.getBody());
    }

    List<String> parseCommentBodies(String body) {
        if (body == null || body.isBlank()) return List.of();
        try {
            JsonNode arr = objectMapper.readTree(body);
            if (!arr.isArray()) return List.of();
            java.util.List<String> result = new java.util.ArrayList<>();
            for (JsonNode comment : arr) {
                String text = comment.path("body").asText(null);
                if (text != null && !text.isBlank()) {
                    result.add(text.trim());
                }
            }
            return result;
        } catch (Exception e) {
            log.error("[AiPrReview] Failed to parse GitHub PR comments: {}", e.getMessage());
            throw new RuntimeException("Malformed GitHub PR comment response", e);
        }
    }

    // ── OpenAI Call ────────────────────────────────────────────────────────────

    private PrReviewDtos.PrReviewResult callOpenAiForReview(List<String> comments) throws Exception {
        String commentsText = String.join("\n\n---\n\n", comments);

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", openAiModel);
        body.put("temperature", 0.1);

        ArrayNode messages = body.putArray("messages");

        // System prompt
        ObjectNode system = messages.addObject();
        system.put("role", "system");
        system.put("content", PR_REVIEW_SYSTEM_PROMPT);

        // User message
        ObjectNode user = messages.addObject();
        user.put("role", "user");
        user.put("content", "PR review comments to evaluate:\n\n" + commentsText);

        // JSON response format
        ObjectNode responseFormat = body.putObject("response_format");
        responseFormat.put("type", "json_object");

        String requestBody = objectMapper.writeValueAsString(body);

        // Direct OpenAI call using the stored (non-encrypted) key for AI services
        // (OpenAI key is not a user token – it's a system key stored in config)
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setBearerAuth(openAiApiKey);
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

        org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(requestBody, headers);
        org.springframework.web.client.RestTemplate rt = new org.springframework.web.client.RestTemplate();
        org.springframework.http.ResponseEntity<String> resp = rt.exchange(
                OPENAI_CHAT_ENDPOINT,
                HttpMethod.POST,
                entity,
                String.class);

        return parseOpenAiResponse(resp.getBody());
    }

    PrReviewDtos.PrReviewResult parseOpenAiResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String content = root.at("/choices/0/message/content").asText(null);
            if (content == null || content.isBlank()) {
                throw new RuntimeException("OpenAI returned empty content for PR review.");
            }
            JsonNode result = objectMapper.readTree(content);
            int score = result.path("review_score").asInt(0);
            boolean meaningful = result.path("is_meaningful").asBoolean(false);
            String summary = result.path("summary").asText("");
            String reasoning = result.path("reasoning").asText("");
            return new PrReviewDtos.PrReviewResult(score, meaningful, summary, reasoning);
        } catch (Exception e) {
            log.error("[AiPrReview] Failed to parse OpenAI response: {}", e.getMessage());
            throw new RuntimeException("Malformed OpenAI PR review response: " + e.getMessage(), e);
        }
    }

    // ── System Prompt ──────────────────────────────────────────────────────────

    private static final String PR_REVIEW_SYSTEM_PROMPT =
            "You are an expert software engineering code-review auditor.\n" +
            "Your task is to evaluate whether the provided pull-request review comments\n" +
            "constitute a MEANINGFUL software engineering review.\n\n" +
            "CRITERIA for a meaningful review:\n" +
            "- Discusses architecture, design patterns, or system design.\n" +
            "- Points out potential bugs, edge cases, or security vulnerabilities.\n" +
            "- Provides constructive suggestions with technical reasoning.\n" +
            "- References specific lines, functions, or modules.\n\n" +
            "CRITERIA for a superficial / meaningless review:\n" +
            "- Comments like 'LGTM', '+1', 'Looks good', 'Approved', 'OK'.\n" +
            "- Vague praise without technical substance.\n" +
            "- Nitpicking whitespace/formatting with no engineering discussion.\n\n" +
            "Scoring rubric (review_score 0-100):\n" +
            "- 80-100: Deep architectural/design discussion, clear technical value.\n" +
            "- 50-79: Some technical substance but incomplete or shallow in places.\n" +
            "- 20-49: Mostly superficial with occasional technical remark.\n" +
            "- 0-19: Entirely superficial (LGTM, '+1', emoji reactions only).\n\n" +
            "is_meaningful = true if review_score >= 40.\n\n" +
            "Output ONLY valid JSON with exactly these keys:\n" +
            "{\"review_score\": <integer 0-100>, \"is_meaningful\": <boolean>, \"summary\": \"<string>\", \"reasoning\": \"<string>\"}";
}
