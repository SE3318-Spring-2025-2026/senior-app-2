package com.seniorapp.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.seniorapp.dto.ai.AlignmentDtos;
import com.seniorapp.dto.comparison.ComparisonDtos;
import com.seniorapp.exception.AiValidationTimeoutException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.*;

/**
 * Level 4 AI Validator – Task ↔ Code Alignment.
 *
 * <p>Provides two public methods:
 * <ul>
 *   <li>{@link #validate} – existing Level 4 base: strict JSON-schema via OpenAI Responses API.</li>
 *   <li>{@link #validateAlignment} – enhanced: compares Jira description with GitHub PR file diffs,
 *       returning {@code alignment_score}, {@code completeness_score}, {@code reasoning}, {@code summary}.</li>
 * </ul>
 *
 * <p>The {@code GradingEngine.java} is NOT modified – it reads its inputs independently.
 */
@Service
public class TaskCodeAlignmentValidatorService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private final String openAiApiKey;
    private final String openAiModel;
    private final long timeoutMs;

    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setName("ai-validator-" + t.getId());
        t.setDaemon(true);
        return t;
    });

    public TaskCodeAlignmentValidatorService(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${openai.api-key:}") String openAiApiKey,
            @Value("${openai.model:gpt-4.1-mini}") String openAiModel,
            @Value("${ai.request-timeout-ms:30000}") long timeoutMs
    ) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.openAiApiKey = openAiApiKey;
        this.openAiModel = openAiModel;
        this.timeoutMs = timeoutMs;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // EXISTING Level 4 base – validate() [DO NOT MODIFY]
    // ═══════════════════════════════════════════════════════════════════════════

    public ComparisonDtos.AIFeedbackItemList validate(
            String jiraSummary,
            String jiraDescription,
            List<String> acceptanceCriteria,
            String gitDiff
    ) {
        if (openAiApiKey == null || openAiApiKey.isBlank()) {
            ComparisonDtos.AIFeedbackItemList fallback = new ComparisonDtos.AIFeedbackItemList();
            fallback.setAccuracyScore(0.0f);
            fallback.setDiscrepancies(List.of("AI configuration missing: openai.api-key"));
            fallback.setEvidence(List.of("Cannot call OpenAI because API key is not configured."));
            return fallback;
        }

        Callable<ComparisonDtos.AIFeedbackItemList> task = () -> callOpenAiAndParse(jiraSummary, jiraDescription, acceptanceCriteria, gitDiff);

        Future<ComparisonDtos.AIFeedbackItemList> future = executor.submit(task);
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new AiValidationTimeoutException("AI validation exceeded timeout of " + timeoutMs + "ms", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof AiValidationTimeoutException) {
                throw (AiValidationTimeoutException) cause;
            }
            throw new RuntimeException("AI validation failed: " + cause.getMessage(), cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("AI validation interrupted", e);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // NEW Level 4 Upgrade – validateAlignment()
    // Compares Jira Issue Description with GitHub PR File Diffs.
    // Returns alignment_score, completeness_score, reasoning, summary.
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Enhanced Level 4 validation: compares a Jira issue description against
     * the actual GitHub PR file diffs to compute alignment and completeness scores.
     *
     * <p>A "fake" PR (code unrelated to the Jira task) should score near 0.0.
     * A partial implementation should score in the 0.3–0.7 range.
     * A complete, aligned implementation should score 0.8+.
     *
     * @param jiraSummary        short Jira task summary / title
     * @param jiraDescription    full Jira task description (may include acceptance criteria)
     * @param acceptanceCriteria explicit acceptance criteria (may be empty)
     * @param prFileDiffs        concatenated git-diff text for the PR files
     * @return structured alignment result
     * @throws AiValidationTimeoutException if OpenAI does not respond within the configured timeout
     */
    public AlignmentDtos.AlignmentResult validateAlignment(
            String jiraSummary,
            String jiraDescription,
            List<String> acceptanceCriteria,
            String prFileDiffs
    ) {
        if (openAiApiKey == null || openAiApiKey.isBlank()) {
            return new AlignmentDtos.AlignmentResult(
                    0.0f, 0.0f,
                    "AI configuration missing: openai.api-key is not set.",
                    "Cannot call OpenAI – API key not configured."
            );
        }

        Callable<AlignmentDtos.AlignmentResult> task = () ->
                callOpenAiForAlignment(jiraSummary, jiraDescription, acceptanceCriteria, prFileDiffs);

        Future<AlignmentDtos.AlignmentResult> future = executor.submit(task);
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new AiValidationTimeoutException(
                    "Level 4 alignment validation exceeded timeout of " + timeoutMs + "ms", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof AiValidationTimeoutException t) throw t;
            throw new RuntimeException("Level 4 alignment validation failed: " + cause.getMessage(), cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Level 4 alignment validation interrupted", e);
        }
    }

    /**
     * Calls OpenAI chat completions API with a structured JSON prompt for alignment scoring.
     */
    private AlignmentDtos.AlignmentResult callOpenAiForAlignment(
            String jiraSummary,
            String jiraDescription,
            List<String> acceptanceCriteria,
            String prFileDiffs
    ) throws Exception {
        String endpoint = "https://api.openai.com/v1/chat/completions";

        ArrayNode criteriaArray = objectMapper.valueToTree(
                acceptanceCriteria != null ? acceptanceCriteria : List.of());

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", openAiModel);
        body.put("temperature", 0.05);

        ObjectNode responseFormat = body.putObject("response_format");
        responseFormat.put("type", "json_object");

        ArrayNode messages = body.putArray("messages");

        ObjectNode system = messages.addObject();
        system.put("role", "system");
        system.put("content", ALIGNMENT_SYSTEM_PROMPT);

        ObjectNode user = messages.addObject();
        user.put("role", "user");
        String userContent =
                "Jira task summary:\n" + jiraSummary + "\n\n" +
                "Jira task description:\n" + (jiraDescription != null ? jiraDescription : "(none)") + "\n\n" +
                "Acceptance criteria:\n" + criteriaArray + "\n\n" +
                "GitHub PR file diffs:\n" + (prFileDiffs != null ? prFileDiffs : "(none)");
        user.put("content", userContent);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(openAiApiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        RequestEntity<String> req = RequestEntity
                .post(endpoint)
                .headers(headers)
                .body(objectMapper.writeValueAsString(body));

        ResponseEntity<String> resp = restTemplate.exchange(req, String.class);
        return parseAlignmentResponse(resp.getBody());
    }

    /**
     * Parses the OpenAI chat-completion response body into an {@link AlignmentDtos.AlignmentResult}.
     * Package-private for testing.
     */
    AlignmentDtos.AlignmentResult parseAlignmentResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String content = root.at("/choices/0/message/content").asText(null);
            if (content == null || content.isBlank()) {
                throw new RuntimeException("OpenAI returned empty content for alignment validation.");
            }
            JsonNode json = objectMapper.readTree(content);
            float alignment    = (float) json.path("alignment_score").asDouble(0.0);
            float completeness = (float) json.path("completeness_score").asDouble(0.0);
            String reasoning   = json.path("reasoning").asText("");
            String summary     = json.path("summary").asText("");
            return new AlignmentDtos.AlignmentResult(alignment, completeness, reasoning, summary);
        } catch (Exception e) {
            throw new RuntimeException("Malformed OpenAI alignment response: " + e.getMessage(), e);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // EXISTING private helper – callOpenAiAndParse() [DO NOT MODIFY]
    // ═══════════════════════════════════════════════════════════════════════════

    private ComparisonDtos.AIFeedbackItemList callOpenAiAndParse(
            String jiraSummary,
            String jiraDescription,
            List<String> acceptanceCriteria,
            String gitDiff
    ) throws Exception {
        String endpoint = "https://api.openai.com/v1/responses";

        ArrayNode criteriaArray = objectMapper.valueToTree(acceptanceCriteria != null ? acceptanceCriteria : List.of());

        ObjectNode inputRoot = objectMapper.createObjectNode();
        inputRoot.put("model", openAiModel);

        ObjectNode responseFormat = objectMapper.createObjectNode();
        responseFormat.put("type", "json_schema");

        ObjectNode jsonSchema = objectMapper.createObjectNode();
        jsonSchema.put("name", "task_code_alignment_audit");
        jsonSchema.put("strict", true);

        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = objectMapper.createObjectNode();

        ObjectNode accuracyScore = objectMapper.createObjectNode();
        accuracyScore.put("type", "number");
        accuracyScore.put("minimum", 0);
        accuracyScore.put("maximum", 1);
        props.set("accuracyScore", accuracyScore);

        ObjectNode discrepancies = objectMapper.createObjectNode();
        discrepancies.put("type", "array");
        discrepancies.put("items", objectMapper.createObjectNode().put("type", "string"));
        props.set("discrepancies", discrepancies);

        ObjectNode evidence = objectMapper.createObjectNode();
        evidence.put("type", "array");
        evidence.put("items", objectMapper.createObjectNode().put("type", "string"));
        props.set("evidence", evidence);

        ObjectNode summary = objectMapper.createObjectNode();
        summary.put("type", "string");
        props.set("summary", summary);

        schema.set("properties", props);

        ArrayNode required = objectMapper.createArrayNode();
        required.add("accuracyScore");
        required.add("discrepancies");
        required.add("evidence");
        required.add("summary");
        schema.set("required", required);

        jsonSchema.set("schema", schema);
        responseFormat.set("json_schema", jsonSchema);
        inputRoot.set("response_format", responseFormat);

        ObjectNode systemMsg = objectMapper.createObjectNode();
        systemMsg.put("role", "system");
        systemMsg.put("content", TaskCodeAlignmentValidatorPrompt.systemPrompt());

        ObjectNode userMsg = objectMapper.createObjectNode();
        userMsg.put("role", "user");
        String userContent = "Jira task summary:\n" + jiraSummary + "\n\n" +
                "Jira task description:\n" + (jiraDescription != null ? jiraDescription : "") + "\n\n" +
                "Acceptance criteria:\n" + criteriaArray.toString() + "\n\n" +
                "Git diff (the developer code change):\n" + gitDiff;
        userMsg.put("content", userContent);

        ArrayNode messages = objectMapper.createArrayNode();
        messages.add(systemMsg);
        messages.add(userMsg);

        inputRoot.set("input", objectMapper.createObjectNode().set("messages", messages));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(openAiApiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        RequestEntity<String> req = RequestEntity
                .post(endpoint)
                .headers(headers)
                .body(objectMapper.writeValueAsString(inputRoot));

        ResponseEntity<String> resp = restTemplate.exchange(req, String.class);

        JsonNode root = objectMapper.readTree(resp.getBody());
        JsonNode content = root.at("/output/0/content/0/text");
        String text = content.isMissingNode() ? null : content.asText();
        if (text == null || text.isBlank()) {
            JsonNode alt = root.at("/output/0/content/0");
            text = alt.isMissingNode() ? null : alt.toString();
        }
        if (text == null || text.isBlank()) {
            throw new RuntimeException("OpenAI response missing content text for JSON parsing.");
        }

        JsonNode json = objectMapper.readTree(text);

        ComparisonDtos.AIFeedbackItemList result = new ComparisonDtos.AIFeedbackItemList();
        result.setAccuracyScore((float) json.path("accuracyScore").asDouble(0.0));

        ArrayNode disc = (ArrayNode) json.path("discrepancies");
        result.setDiscrepancies(objectMapper.convertValue(disc, List.class));

        ArrayNode ev = (ArrayNode) json.path("evidence");
        result.setEvidence(objectMapper.convertValue(ev, List.class));

        result.setSummary(json.path("summary").asText(""));
        return result;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // System Prompt for validateAlignment()
    // ═══════════════════════════════════════════════════════════════════════════

    private static final String ALIGNMENT_SYSTEM_PROMPT =
            "You are a Senior Technical Auditor specialising in software engineering delivery.\n" +
            "Your job is to compare a Jira task specification with a GitHub PR diff and produce TWO scores:\n\n" +
            "1. alignment_score (0.0-1.0): Does the code change actually address the Jira task?\n" +
            "   - 1.0 = code perfectly matches task intent and acceptance criteria.\n" +
            "   - 0.0 = code is completely unrelated to the task ('fake' PR).\n\n" +
            "2. completeness_score (0.0-1.0): What fraction of acceptance criteria are covered?\n" +
            "   - 1.0 = all criteria satisfied by the diff.\n" +
            "   - 0.5 = roughly half the criteria are covered (partial implementation).\n" +
            "   - 0.0 = none of the criteria are covered.\n\n" +
            "FAKE PR DETECTION:\n" +
            "If the diff adds/modifies code that has no logical relationship to the Jira description,\n" +
            "set alignment_score <= 0.1 and completeness_score <= 0.1.\n\n" +
            "PARTIAL IMPLEMENTATION:\n" +
            "If only some acceptance criteria are addressed, set completeness_score proportionally.\n\n" +
            "Output ONLY valid JSON:\n" +
            "{\"alignment_score\":<float>,\"completeness_score\":<float>,\"reasoning\":\"<string>\",\"summary\":\"<string>\"}";
}
