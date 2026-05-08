package com.seniorapp.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.seniorapp.dto.comparison.ComparisonDtos;
import com.seniorapp.exception.AiValidationTimeoutException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;

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

    public ComparisonDtos.AIFeedbackItemList validate(
            String jiraSummary,
            String jiraDescription,
            List<String> acceptanceCriteria,
            String gitDiff
    ) {
        if (openAiApiKey == null || openAiApiKey.isBlank()) {
            // Fail fast with a deterministic output so pipeline remains parseable.
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

        // Provide strict JSON schema via response_format
        ObjectNode responseFormat = objectMapper.createObjectNode();
        responseFormat.put("type", "json_schema");

        ObjectNode jsonSchema = objectMapper.createObjectNode();
        jsonSchema.put("name", "task_code_alignment_audit");
        jsonSchema.put("strict", true);

        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode props = objectMapper.createObjectNode();

        // accuracyScore
        ObjectNode accuracyScore = objectMapper.createObjectNode();
        accuracyScore.put("type", "number");
        accuracyScore.put("minimum", 0);
        accuracyScore.put("maximum", 1);
        props.set("accuracyScore", accuracyScore);

        // discrepancies
        ObjectNode discrepancies = objectMapper.createObjectNode();
        discrepancies.put("type", "array");
        discrepancies.put("items", objectMapper.createObjectNode().put("type", "string"));
        props.set("discrepancies", discrepancies);

        // evidence
        ObjectNode evidence = objectMapper.createObjectNode();
        evidence.put("type", "array");
        evidence.put("items", objectMapper.createObjectNode().put("type", "string"));
        props.set("evidence", evidence);

        // summary
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

        // Top-level instructions
        ObjectNode system = objectMapper.createObjectNode();
        system.put("role", "system");
        system.put("content", TaskCodeAlignmentValidatorPrompt.systemPrompt());

        ObjectNode user = objectMapper.createObjectNode();
        user.put("role", "user");

        String userContent = "Jira task summary:\n" + jiraSummary + "\n\n" +
                "Jira task description:\n" + (jiraDescription != null ? jiraDescription : "") + "\n\n" +
                "Acceptance criteria:\n" + criteriaArray.toString() + "\n\n" +
                "Git diff (the developer code change):\n" + gitDiff;

        user.put("content", userContent);

        ArrayNode messages = objectMapper.createArrayNode();
        messages.add(system);
        messages.add(user);

        inputRoot.set("input", objectMapper.createObjectNode().set("messages", messages));

        ObjectNode request = inputRoot;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(openAiApiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        RequestEntity<String> req = RequestEntity
                .post(endpoint)
                .headers(headers)
                .body(objectMapper.writeValueAsString(request));

        ResponseEntity<String> resp = restTemplate.exchange(req, String.class);

        JsonNode root = objectMapper.readTree(resp.getBody());

        // Responses API returns output in a content array. We'll locate first JSON object-like text.
        // This is provider-dependent; we still keep parsing robust.
        JsonNode output = root.at("/output");
        JsonNode content = root.at("/output/0/content/0/text");
        String text = content.isMissingNode() ? null : content.asText();
        if (text == null || text.isBlank()) {
            // Fallback: try to parse some other text field
            JsonNode alt = root.at("/output/0/content/0");
            text = alt.isMissingNode() ? null : alt.toString();
        }
        if (text == null || text.isBlank()) {
            throw new RuntimeException("OpenAI response missing content text for JSON parsing.");
        }

        // The text itself should already be strict JSON per schema.
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
}

