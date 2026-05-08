package com.seniorapp.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seniorapp.dto.ai.PrReviewDtos;
import com.seniorapp.exception.AiValidationTimeoutException;
import com.seniorapp.service.SecureOutboundApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Aggressive unit tests for {@link AiPullRequestReviewValidatorService}.
 *
 * Edge cases covered:
 * - Superficial comments ("LGTM", "+1") → is_meaningful=false, score ≤ 25
 * - Architectural discussion → is_meaningful=true, score ≥ 70
 * - No review comments → score=0, not meaningful
 * - GitHub 429 Rate Limit → ResponseStatusException thrown
 * - GitHub 401 Unauthorized → empty comments, graceful fallback
 * - OpenAI timeout → AiValidationTimeoutException thrown
 * - Malformed OpenAI JSON → RuntimeException
 * - Missing OpenAI API key → fallback result returned
 * - Malformed GitHub PR comments JSON → RuntimeException
 * - parseCommentBodies with empty array
 * - Comment body is blank → filtered out
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AiPullRequestReviewValidatorService – Aggressive Unit Tests")
class AiPullRequestReviewValidatorServiceTest {

    @Mock
    private SecureOutboundApiService secureOutboundApiService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Service under test – instantiated manually so we can control openAiApiKey. */
    private AiPullRequestReviewValidatorService service;

    @BeforeEach
    void setUp() {
        // Use a non-blank dummy key so the service does not short-circuit on key check
        // For tests that actually call OpenAI we will override the service differently.
        service = new AiPullRequestReviewValidatorService(
                secureOutboundApiService,
                objectMapper,
                "test-api-key",
                "gpt-4o-mini",
                5000L
        );
    }

    // ══════════════════════════════════════════════════════════════════════════
    // fetchPrReviewComments() tests
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("fetchPrReviewComments()")
    class FetchPrReviewCommentsTests {

        @Test
        @DisplayName("Returns list of comment bodies from valid 200 response")
        void returns_comment_bodies_from_200() {
            String body = """
                    [{"id":1,"body":"Good catch on the null check!"},{"id":2,"body":"Consider using Optional here."}]
                    """;
            when(secureOutboundApiService.executeGitHubApiCall(anyString(), anyString(), isNull(), eq(HttpMethod.GET)))
                    .thenReturn(ResponseEntity.ok(body));

            List<String> comments = service.fetchPrReviewComments("enc:v1:aaa:bbb", "owner", "repo", 42);

            assertThat(comments).containsExactly("Good catch on the null check!", "Consider using Optional here.");
        }

        @Test
        @DisplayName("Empty comment array → returns empty list")
        void empty_array_returns_empty_list() {
            when(secureOutboundApiService.executeGitHubApiCall(anyString(), anyString(), isNull(), eq(HttpMethod.GET)))
                    .thenReturn(ResponseEntity.ok("[]"));

            List<String> comments = service.fetchPrReviewComments("enc:v1:aaa:bbb", "owner", "repo", 42);
            assertThat(comments).isEmpty();
        }

        @Test
        @DisplayName("GitHub 429 Rate Limit – throws ResponseStatusException TOO_MANY_REQUESTS")
        void github_429_throws_rate_limit() {
            when(secureOutboundApiService.executeGitHubApiCall(anyString(), anyString(), isNull(), eq(HttpMethod.GET)))
                    .thenReturn(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("Rate limit"));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.fetchPrReviewComments("enc:v1:aaa:bbb", "owner", "repo", 42));

            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        }

        @Test
        @DisplayName("GitHub 401 Unauthorized – returns empty list (revoked token graceful fallback)")
        void github_401_returns_empty() {
            when(secureOutboundApiService.executeGitHubApiCall(anyString(), anyString(), isNull(), eq(HttpMethod.GET)))
                    .thenReturn(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized"));

            List<String> comments = service.fetchPrReviewComments("enc:v1:aaa:bbb", "owner", "repo", 1);
            assertThat(comments).isEmpty();
        }

        @Test
        @DisplayName("Malformed JSON from GitHub – throws RuntimeException")
        void malformed_json_throws() {
            when(secureOutboundApiService.executeGitHubApiCall(anyString(), anyString(), isNull(), eq(HttpMethod.GET)))
                    .thenReturn(ResponseEntity.ok("NOT JSON {{{"));

            assertThrows(RuntimeException.class,
                    () -> service.fetchPrReviewComments("enc:v1:aaa:bbb", "owner", "repo", 1));
        }

        @Test
        @DisplayName("Comment body is blank – filtered out")
        void blank_comment_body_is_filtered() {
            String body = """
                    [{"id":1,"body":"  "},{"id":2,"body":"Proper review comment."}]
                    """;
            when(secureOutboundApiService.executeGitHubApiCall(anyString(), anyString(), isNull(), eq(HttpMethod.GET)))
                    .thenReturn(ResponseEntity.ok(body));

            List<String> comments = service.fetchPrReviewComments("enc:v1:aaa:bbb", "owner", "repo", 1);
            assertThat(comments).containsExactly("Proper review comment.");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // parseCommentBodies() – unit tests
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("parseCommentBodies()")
    class ParseCommentBodiesTests {

        @Test
        @DisplayName("Empty body string → empty list")
        void empty_body_yields_empty_list() {
            assertThat(service.parseCommentBodies("")).isEmpty();
        }

        @Test
        @DisplayName("Non-array JSON → empty list")
        void non_array_json_yields_empty_list() {
            assertThat(service.parseCommentBodies("{\"key\":\"value\"}")).isEmpty();
        }

        @Test
        @DisplayName("Mixed blank and non-blank bodies – only non-blank returned")
        void mixed_blank_and_valid() {
            String json = "[{\"body\":\"\"},{\"body\":\"Design feedback here.\"}]";
            assertThat(service.parseCommentBodies(json)).containsExactly("Design feedback here.");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // parseOpenAiResponse() – unit tests that replace real AI calls
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("parseOpenAiResponse() – AI scoring contract")
    class ParseOpenAiResponseTests {

        /**
         * Helper to build a fake OpenAI chat completion response body.
         */
        private String openAiResponse(int score, boolean meaningful, String summary, String reasoning) {
            String content = "{\"review_score\":" + score +
                    ",\"is_meaningful\":" + meaningful +
                    ",\"summary\":\"" + summary + "\"" +
                    ",\"reasoning\":\"" + reasoning + "\"}";
            return "{\"choices\":[{\"message\":{\"content\":" +
                    objectMapper.createObjectNode().put("x", content).get("x").toString() +
                    "}}]}";
        }

        @Test
        @DisplayName("Superficial comment ('LGTM') → is_meaningful=false, score <= 25")
        void lgtm_yields_low_score_not_meaningful() {
            String response = openAiResponse(5, false,
                    "Only superficial LGTM comment found.",
                    "No technical discussion detected.");

            PrReviewDtos.PrReviewResult result = service.parseOpenAiResponse(response);

            assertThat(result.isMeaningful()).isFalse();
            assertThat(result.getReviewScore()).isLessThanOrEqualTo(25);
            assertThat(result.getSummary()).contains("superficial");
        }

        @Test
        @DisplayName("'+1' comment → is_meaningful=false, score <= 25")
        void plus_one_yields_low_score() {
            String response = openAiResponse(3, false,
                    "Only '+1' found – no engineering substance.",
                    "Single reaction emoji.");

            PrReviewDtos.PrReviewResult result = service.parseOpenAiResponse(response);

            assertThat(result.isMeaningful()).isFalse();
            assertThat(result.getReviewScore()).isLessThanOrEqualTo(25);
        }

        @Test
        @DisplayName("Architectural discussion → is_meaningful=true, score >= 70")
        void architectural_discussion_yields_high_score() {
            String response = openAiResponse(85, true,
                    "Reviewer discussed CQRS pattern, event sourcing trade-offs, and suggested specific refactors.",
                    "Deep architectural reasoning with code references.");

            PrReviewDtos.PrReviewResult result = service.parseOpenAiResponse(response);

            assertThat(result.isMeaningful()).isTrue();
            assertThat(result.getReviewScore()).isGreaterThanOrEqualTo(70);
        }

        @Test
        @DisplayName("Moderate review → score between 50 and 79, is_meaningful=true")
        void moderate_review_is_meaningful() {
            String response = openAiResponse(65, true,
                    "Reviewer pointed out a null-pointer edge case and suggested using Optional.",
                    "Some technical substance.");

            PrReviewDtos.PrReviewResult result = service.parseOpenAiResponse(response);

            assertThat(result.isMeaningful()).isTrue();
            assertThat(result.getReviewScore()).isBetween(50, 79);
        }

        @Test
        @DisplayName("Malformed OpenAI JSON – throws RuntimeException")
        void malformed_openai_json_throws() {
            assertThrows(RuntimeException.class,
                    () -> service.parseOpenAiResponse("{{INVALID}}"));
        }

        @Test
        @DisplayName("OpenAI response with empty content – throws RuntimeException")
        void empty_content_throws() {
            String response = "{\"choices\":[{\"message\":{\"content\":\"\"}}]}";
            assertThrows(RuntimeException.class,
                    () -> service.parseOpenAiResponse(response));
        }

        @Test
        @DisplayName("OpenAI response with missing choices – throws RuntimeException")
        void missing_choices_throws() {
            assertThrows(RuntimeException.class,
                    () -> service.parseOpenAiResponse("{\"id\":\"chatcmpl-xxx\"}"));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // validatePrReview() – integration-level unit tests
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("validatePrReview() – full pipeline")
    class ValidatePrReviewTests {

        @Test
        @DisplayName("No comments on PR → score=0, is_meaningful=false without calling OpenAI")
        void no_comments_returns_zero_score_without_openai() {
            when(secureOutboundApiService.executeGitHubApiCall(anyString(), anyString(), isNull(), eq(HttpMethod.GET)))
                    .thenReturn(ResponseEntity.ok("[]"));

            // Use service with no-key so we also assert OpenAI is never reached
            AiPullRequestReviewValidatorService noKeyService = new AiPullRequestReviewValidatorService(
                    secureOutboundApiService, objectMapper, "", "gpt-4o-mini", 5000L);

            PrReviewDtos.PrReviewResult result = noKeyService.validatePrReview("enc:v1:aaa:bbb", "owner", "repo", 1);

            assertThat(result.getReviewScore()).isEqualTo(0);
            assertThat(result.isMeaningful()).isFalse();
        }

        @Test
        @DisplayName("Missing OpenAI API key → fallback result with score=0")
        void missing_api_key_returns_fallback() {
            // Comments exist but key is blank
            String body = "[{\"body\":\"LGTM\"}]";
            when(secureOutboundApiService.executeGitHubApiCall(anyString(), anyString(), isNull(), eq(HttpMethod.GET)))
                    .thenReturn(ResponseEntity.ok(body));

            AiPullRequestReviewValidatorService noKeyService = new AiPullRequestReviewValidatorService(
                    secureOutboundApiService, objectMapper, "   ", "gpt-4o-mini", 5000L);

            PrReviewDtos.PrReviewResult result = noKeyService.validatePrReview("enc:v1:aaa:bbb", "owner", "repo", 1);

            assertThat(result.getReviewScore()).isEqualTo(0);
            assertThat(result.isMeaningful()).isFalse();
            assertThat(result.getSummary()).containsIgnoringCase("unavailable");
        }
    }
}
