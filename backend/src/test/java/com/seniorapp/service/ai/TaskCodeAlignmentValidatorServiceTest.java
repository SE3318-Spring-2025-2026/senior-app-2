package com.seniorapp.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seniorapp.dto.ai.AlignmentDtos;
import com.seniorapp.dto.comparison.ComparisonDtos;
import com.seniorapp.exception.AiValidationTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Aggressive unit tests for the enhanced {@link TaskCodeAlignmentValidatorService}.
 *
 * Edge cases covered:
 * - Fake PR detection: code diff unrelated to Jira -> alignment_score <= 0.1
 * - Partial implementation: some criteria missing -> completeness_score in 0.3-0.7
 * - Full implementation: all criteria met -> both scores near 1.0
 * - Missing OpenAI API key -> fallback result with both scores = 0.0
 * - OpenAI connection error (SocketTimeout) -> RuntimeException wrapping
 * - Malformed OpenAI response -> RuntimeException
 * - Empty content in OpenAI response -> RuntimeException
 * - Missing alignment_score / completeness_score fields -> defaults to 0.0
 * - Null prFileDiffs -> handled gracefully
 * - Null jiraDescription -> handled gracefully
 * - Existing validate() method still returns AIFeedbackItemList (regression)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TaskCodeAlignmentValidatorService - Level 4 Upgrade Tests")
class TaskCodeAlignmentValidatorServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private TaskCodeAlignmentValidatorService service;

    @BeforeEach
    void setUp() {
        service = new TaskCodeAlignmentValidatorService(
                restTemplate, objectMapper,
                "test-api-key",
                "gpt-4o-mini",
                5000L
        );
    }

    // ══════════════════════════════════════════════════════════════════════════
    // parseAlignmentResponse() - unit tests (no HTTP calls)
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("parseAlignmentResponse() - Score contract unit tests")
    class ParseAlignmentResponseTests {

        @Test
        @DisplayName("Fake PR detection: alignment_score=0.05, completeness_score=0.05")
        void fake_pr_yields_very_low_scores() {
            String response = alignmentResponse(0.05f, 0.05f,
                    "The diff modifies CSS styling while the Jira task requests a new authentication endpoint.",
                    "Completely unrelated code change.");

            AlignmentDtos.AlignmentResult result = service.parseAlignmentResponse(response);

            assertThat(result.getAlignmentScore()).isLessThanOrEqualTo(0.1f);
            assertThat(result.getCompletenessScore()).isLessThanOrEqualTo(0.1f);
            // Either reasoning or summary should convey the mismatch
            assertThat(result.getReasoning() + " " + result.getSummary())
                    .containsIgnoringCase("CSS");
        }

        @Test
        @DisplayName("Partial implementation: completeness_score in 0.3-0.7")
        void partial_implementation_scores_mid_range() {
            String response = alignmentResponse(0.75f, 0.5f,
                    "Login endpoint added but logout and session expiry not implemented.",
                    "Partial implementation covering 2 of 4 criteria.");

            AlignmentDtos.AlignmentResult result = service.parseAlignmentResponse(response);

            assertThat(result.getAlignmentScore()).isGreaterThan(0.5f);
            assertThat(result.getCompletenessScore()).isBetween(0.3f, 0.7f);
        }

        @Test
        @DisplayName("Full implementation: both scores near 1.0")
        void full_implementation_scores_near_one() {
            String response = alignmentResponse(0.95f, 0.95f,
                    "All CRUD endpoints implemented with tests.",
                    "Complete and aligned implementation.");

            AlignmentDtos.AlignmentResult result = service.parseAlignmentResponse(response);

            assertThat(result.getAlignmentScore()).isGreaterThanOrEqualTo(0.8f);
            assertThat(result.getCompletenessScore()).isGreaterThanOrEqualTo(0.8f);
        }

        @Test
        @DisplayName("Missing alignment_score field -> defaults to 0.0")
        void missing_alignment_score_defaults_to_zero() {
            String response = openAiResponse("{\"completeness_score\":0.5,\"reasoning\":\"r\",\"summary\":\"s\"}");
            AlignmentDtos.AlignmentResult result = service.parseAlignmentResponse(response);
            assertThat(result.getAlignmentScore()).isEqualTo(0.0f);
            assertThat(result.getCompletenessScore()).isEqualTo(0.5f);
        }

        @Test
        @DisplayName("Missing completeness_score field -> defaults to 0.0")
        void missing_completeness_score_defaults_to_zero() {
            String response = openAiResponse("{\"alignment_score\":0.8,\"reasoning\":\"r\",\"summary\":\"s\"}");
            AlignmentDtos.AlignmentResult result = service.parseAlignmentResponse(response);
            assertThat(result.getCompletenessScore()).isEqualTo(0.0f);
        }

        @Test
        @DisplayName("Malformed JSON -> throws RuntimeException")
        void malformed_json_throws() {
            assertThrows(RuntimeException.class,
                    () -> service.parseAlignmentResponse("{MALFORMED}"));
        }

        @Test
        @DisplayName("Empty content in OpenAI response -> throws RuntimeException")
        void empty_content_throws() {
            String response = "{\"choices\":[{\"message\":{\"content\":\"\"}}]}";
            assertThrows(RuntimeException.class,
                    () -> service.parseAlignmentResponse(response));
        }

        @Test
        @DisplayName("Null body -> throws RuntimeException")
        void null_body_throws() {
            assertThrows(RuntimeException.class,
                    () -> service.parseAlignmentResponse(null));
        }

        @Test
        @DisplayName("Missing choices node -> throws RuntimeException")
        void missing_choices_throws() {
            assertThrows(RuntimeException.class,
                    () -> service.parseAlignmentResponse("{\"id\":\"chatcmpl-abc\"}"));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // validateAlignment() - integration-level unit tests with mocked RestTemplate
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("validateAlignment() - pipeline tests")
    class ValidateAlignmentTests {

        @Test
        @DisplayName("Missing API key -> fallback result with both scores=0.0")
        void missing_api_key_returns_fallback() {
            TaskCodeAlignmentValidatorService noKeyService = new TaskCodeAlignmentValidatorService(
                    restTemplate, objectMapper, "", "gpt-4o-mini", 5000L);

            AlignmentDtos.AlignmentResult result = noKeyService.validateAlignment(
                    "Add login API", "Implement JWT auth", List.of("POST /login returns JWT"), "+ console.log()");

            assertThat(result.getAlignmentScore()).isEqualTo(0.0f);
            assertThat(result.getCompletenessScore()).isEqualTo(0.0f);
            assertThat(result.getSummary()).containsIgnoringCase("configured");
            verifyNoInteractions(restTemplate);
        }

        @Test
        @DisplayName("Blank API key -> fallback result")
        void blank_api_key_returns_fallback() {
            TaskCodeAlignmentValidatorService blankKeyService = new TaskCodeAlignmentValidatorService(
                    restTemplate, objectMapper, "   ", "gpt-4o-mini", 5000L);

            AlignmentDtos.AlignmentResult result = blankKeyService.validateAlignment(
                    "Fix bug", null, List.of(), null);

            assertThat(result.getAlignmentScore()).isEqualTo(0.0f);
            verifyNoInteractions(restTemplate);
        }

        @Test
        @DisplayName("Successful OpenAI response -> parses alignment and completeness scores")
        void successful_response_parsed_correctly() {
            String mockResponse = alignmentResponse(0.9f, 0.85f,
                    "JWT auth endpoint matches task spec.", "Well-aligned implementation.");

            when(restTemplate.exchange(any(RequestEntity.class), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(mockResponse));

            AlignmentDtos.AlignmentResult result = service.validateAlignment(
                    "Add JWT auth", "Implement login with JWT",
                    List.of("POST /login returns token"),
                    "+    return Jwts.builder().setSubject(email).compact();");

            assertThat(result.getAlignmentScore()).isGreaterThan(0.8f);
            assertThat(result.getCompletenessScore()).isGreaterThan(0.8f);
        }

        @Test
        @DisplayName("Fake PR diff -> alignment_score <= 0.1, completeness_score <= 0.1")
        void fake_pr_diff_yields_low_scores() {
            String fakeResponse = alignmentResponse(0.02f, 0.02f,
                    "Diff modifies unrelated CSS. No authentication code present.",
                    "Unrelated PR.");

            when(restTemplate.exchange(any(RequestEntity.class), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(fakeResponse));

            AlignmentDtos.AlignmentResult result = service.validateAlignment(
                    "Implement JWT login endpoint",
                    "Create POST /api/auth/login returning JWT token",
                    List.of("Returns 200 with JWT", "Returns 401 for wrong credentials"),
                    "+  body { color: red; }"
            );

            assertThat(result.getAlignmentScore()).isLessThanOrEqualTo(0.1f);
            assertThat(result.getCompletenessScore()).isLessThanOrEqualTo(0.1f);
        }

        @Test
        @DisplayName("OpenAI connection error (SocketTimeout) -> RuntimeException thrown")
        void openai_connection_error_throws_runtime_exception() {
            when(restTemplate.exchange(any(RequestEntity.class), eq(String.class)))
                    .thenThrow(new ResourceAccessException("timeout",
                            new SocketTimeoutException("read timeout")));

            assertThrows(RuntimeException.class,
                    () -> service.validateAlignment("task", "desc", List.of(), "diff"));
        }

        @Test
        @DisplayName("Null prFileDiffs -> handled gracefully (no NullPointerException)")
        void null_pr_file_diffs_handled_gracefully() {
            String response = alignmentResponse(0.0f, 0.0f, "No diff provided.", "Empty diff.");
            when(restTemplate.exchange(any(RequestEntity.class), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(response));

            assertDoesNotThrow(() -> service.validateAlignment(
                    "Task summary", "Task description", List.of(), null));
        }

        @Test
        @DisplayName("Null jiraDescription -> handled gracefully (no NullPointerException)")
        void null_jira_description_handled_gracefully() {
            String response = alignmentResponse(0.5f, 0.5f, "Partial match.", "OK.");
            when(restTemplate.exchange(any(RequestEntity.class), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(response));

            assertDoesNotThrow(() -> service.validateAlignment(
                    "Task summary", null, null, "+  some.code();"));
        }

        @Test
        @DisplayName("AiValidationTimeoutException is thrown when future times out")
        void future_timeout_throws_ai_validation_timeout_exception() throws Exception {
            // Simulate a slow OpenAI response by blocking the thread
            when(restTemplate.exchange(any(RequestEntity.class), eq(String.class)))
                    .thenAnswer(inv -> {
                        Thread.sleep(200);  // sleep longer than timeout
                        return ResponseEntity.ok("");
                    });

            TaskCodeAlignmentValidatorService veryShortTimeoutService =
                    new TaskCodeAlignmentValidatorService(
                            restTemplate, objectMapper, "key", "gpt-4o-mini", 10L);

            assertThrows(AiValidationTimeoutException.class,
                    () -> veryShortTimeoutService.validateAlignment(
                            "task", "desc", List.of(), "diff"));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Regression: existing validate() method still works
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Regression: existing validate() - AIFeedbackItemList")
    class ExistingValidateRegressionTests {

        @Test
        @DisplayName("Missing API key -> returns fallback AIFeedbackItemList with score=0.0")
        void missing_key_returns_fallback() {
            TaskCodeAlignmentValidatorService noKeyService = new TaskCodeAlignmentValidatorService(
                    restTemplate, objectMapper, "", "gpt-4o-mini", 5000L);

            ComparisonDtos.AIFeedbackItemList result = noKeyService.validate(
                    "Fix NPE", "Null check missing", List.of(), "- return obj.getId();");

            assertThat(result.getAccuracyScore()).isEqualTo(0.0f);
            assertThat(result.getDiscrepancies()).contains("AI configuration missing: openai.api-key");
            verifyNoInteractions(restTemplate);
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private String alignmentResponse(float alignment, float completeness,
                                     String reasoning, String summary) {
        // Use Locale.US to ensure '.' decimal separator (not ',' which breaks JSON)
        String content = String.format(java.util.Locale.US,
                "{\"alignment_score\":%.2f,\"completeness_score\":%.2f,\"reasoning\":\"%s\",\"summary\":\"%s\"}",
                alignment, completeness,
                reasoning.replace("\"", "'"),
                summary.replace("\"", "'"));
        return openAiResponse(content);
    }

    private String openAiResponse(String content) {
        return "{\"choices\":[{\"message\":{\"content\":" +
                objectMapper.createObjectNode().put("c", content).get("c").toString() + "}}]}";
    }
}
