package com.seniorapp.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seniorapp.dto.comparison.ComparisonDtos;
import com.seniorapp.exception.AiValidationTimeoutException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TaskCodeAlignmentValidatorServiceTest {

    @Test
    void validate_shouldReturnAccuracyScore_whenValidStrictJsonIsReturned() {
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        ObjectMapper objectMapper = new ObjectMapper();

        // Minimal OpenAI Responses-like payload containing strict JSON text.
        String jsonText = "{\"accuracyScore\":0.85,\"discrepancies\":[\"MISSING_REQUIREMENT\"],\"evidence\":[\"Diff shows input validation\"],\"summary\":\"Aligned\"}";
        String openAiResponse = "{" +
                "\"output\":[{" +
                "\"content\":[{" +
                "\"type\":\"output_text\",\"text\":\"" + jsonText.replace("\"", "\\\"") + "\"" +
                "}]" +
                "}]" +
                "}";

        Mockito.when(restTemplate.exchange(Mockito.any(), Mockito.eq(String.class)))
                .thenReturn(ResponseEntity.ok(openAiResponse));

        TaskCodeAlignmentValidatorService svc = new TaskCodeAlignmentValidatorService(
                restTemplate,
                objectMapper,
                "test-key",
                "gpt-4.1-mini",
                30000
        );

        ComparisonDtos.AIFeedbackItemList result = svc.validate(
                "Summary",
                "Desc",
                List.of("Criterion 1"),
                "diff --git a/a b/a"
        );

        assertNotNull(result);
        assertEquals(0.85f, result.getAccuracyScore());
        assertEquals(List.of("MISSING_REQUIREMENT"), result.getDiscrepancies());
        assertEquals(List.of("Diff shows input validation"), result.getEvidence());
        assertEquals("Aligned", result.getSummary());
    }

    @Test
    void validate_shouldThrowAiValidationTimeoutException_whenTimeoutOccurs() {
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        ObjectMapper objectMapper = new ObjectMapper();

        // Simulate a call that blocks by never returning; our timeout is enforced by Future.get timeout.
        Mockito.when(restTemplate.exchange(Mockito.any(), Mockito.eq(String.class)))
                .thenAnswer(invocation -> {
                    Thread.sleep(200);
                    return ResponseEntity.ok("{}");
                });

        TaskCodeAlignmentValidatorService svc = new TaskCodeAlignmentValidatorService(
                restTemplate,
                objectMapper,
                "test-key",
                "gpt-4.1-mini",
                1 // very small timeout
        );

        assertThrows(AiValidationTimeoutException.class, () -> svc.validate(
                "Summary",
                "Desc",
                List.of("Criterion 1"),
                "diff --git a/a b/a"
        ));
    }
}

