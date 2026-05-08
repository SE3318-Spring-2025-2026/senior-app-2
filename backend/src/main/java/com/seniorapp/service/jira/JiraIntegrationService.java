package com.seniorapp.service.jira;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seniorapp.dto.jira.JiraDtos;
import com.seniorapp.service.SecureOutboundApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Jira Integration Service.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Fetch active sprints for a Jira board.</li>
 *   <li>Fetch issues in an active sprint using JQL.</li>
 *   <li>Extract story-point values from Jira custom fields, handling the case where
 *       the custom field is absent or differently named.</li>
 * </ul>
 *
 * <p>All outbound HTTP calls go through {@link SecureOutboundApiService} so that
 * tokens are decrypted just-in-time and never logged.
 *
 * <p>The Jira token passed to this service must be an <em>encrypted</em>
 * {@code email:api_token} Basic-Auth credential as produced by
 * {@link com.seniorapp.service.IntegrationCredentialCryptoService#encrypt(String)}.
 */
@Service
public class JiraIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(JiraIntegrationService.class);

    /** Jira REST API v3 base path. The host is provided per-call. */
    public static final String REST_API_BASE   = "/rest/api/3";
    public static final String AGILE_API_BASE  = "/rest/agile/1.0";

    /**
     * Known Jira story-point custom field keys, checked in order.
     * Jira Cloud uses {@code story_points} in some configurations and
     * {@code customfield_10016} in others.
     */
    public static final List<String> STORY_POINTS_FIELD_CANDIDATES = List.of(
            "story_points",
            "customfield_10016",
            "customfield_10028",
            "storyPoints"
    );

    private final SecureOutboundApiService secureOutboundApiService;
    private final ObjectMapper objectMapper;

    public JiraIntegrationService(SecureOutboundApiService secureOutboundApiService,
                                  ObjectMapper objectMapper) {
        this.secureOutboundApiService = secureOutboundApiService;
        this.objectMapper = objectMapper;
    }

    // ── Active Sprints ─────────────────────────────────────────────────────────

    /**
     * Returns active sprints for the given Jira board.
     *
     * @param jiraDomain       Jira Cloud host, e.g. {@code acme.atlassian.net}
     * @param encryptedToken   AES-encrypted {@code email:api_token} credential
     * @param boardId          Agile board ID
     * @return list of active sprints (may be empty)
     * @throws ResponseStatusException HTTP 429 if rate-limited; 401 if token revoked
     */
    public List<JiraDtos.Sprint> fetchActiveSprints(String jiraDomain,
                                                    String encryptedToken,
                                                    Long boardId) {
        String url = buildUrl(jiraDomain, AGILE_API_BASE + "/board/" + boardId + "/sprint?state=active");
        ResponseEntity<String> response = secureOutboundApiService
                .executeJiraApiCall(encryptedToken, url, null, HttpMethod.GET);

        assertSuccess(response, "fetchActiveSprints");

        try {
            JiraDtos.SprintSearchResponse parsed =
                    objectMapper.readValue(response.getBody(), JiraDtos.SprintSearchResponse.class);
            List<JiraDtos.Sprint> values = parsed.getValues();
            return values != null ? values : List.of();
        } catch (Exception e) {
            log.error("Failed to parse Jira sprint response: {}", e.getMessage());
            throw new RuntimeException("Malformed Jira sprint response: " + e.getMessage(), e);
        }
    }

    // ── Issues by JQL ──────────────────────────────────────────────────────────

    /**
     * Fetches issues matching the given JQL query.
     *
     * <p>Story points are extracted from the first matching custom field candidate.
     * If no candidate field is present the story-point value is left {@code null}
     * (rather than throwing) so the caller can handle missing fields gracefully.
     *
     * @param jiraDomain     Jira Cloud host
     * @param encryptedToken encrypted credential
     * @param jql            Jira Query Language string
     * @return list of parsed issues with story points populated where found
     */
    public List<JiraDtos.Issue> fetchIssuesByJql(String jiraDomain,
                                                  String encryptedToken,
                                                  String jql) {
        String encodedJql = URLEncoder.encode(jql, StandardCharsets.UTF_8);
        // Request all known story-point field names so Jira returns them in the response
        String fields = "summary,description,status,priority,assignee,"
                + String.join(",", STORY_POINTS_FIELD_CANDIDATES);
        String url = buildUrl(jiraDomain,
                REST_API_BASE + "/search?jql=" + encodedJql + "&fields=" + fields + "&maxResults=100");

        ResponseEntity<String> response = secureOutboundApiService
                .executeJiraApiCall(encryptedToken, url, null, HttpMethod.GET);

        assertSuccess(response, "fetchIssuesByJql");

        return parseIssuesWithStoryPoints(response.getBody());
    }

    /**
     * Builds the JQL string for fetching open sprint issues in a given Jira project.
     *
     * @param projectKey Jira project key, e.g. {@code "MYAPP"}
     * @return JQL string
     */
    public String buildOpenSprintJql(String projectKey) {
        return "project = \"" + projectKey + "\" AND sprint in openSprints()";
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /**
     * Parses the raw Jira issue search JSON and extracts story-point values from
     * whichever custom field candidate is present in each issue.
     */
    List<JiraDtos.Issue> parseIssuesWithStoryPoints(String body) {
        if (body == null || body.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode issuesNode = root.path("issues");
            if (!issuesNode.isArray()) {
                return List.of();
            }

            List<JiraDtos.Issue> issues = new ArrayList<>();
            for (JsonNode issueNode : issuesNode) {
                JiraDtos.Issue issue = new JiraDtos.Issue();
                issue.setId(issueNode.path("id").asText(null));
                issue.setKey(issueNode.path("key").asText(null));

                JsonNode fieldsNode = issueNode.path("fields");
                JiraDtos.IssueFields fields = objectMapper.treeToValue(fieldsNode, JiraDtos.IssueFields.class);

                // Story-point extraction: try each candidate field name
                Double storyPoints = extractStoryPoints(fieldsNode);
                if (storyPoints == null) {
                    log.debug("Issue {} has no story-point custom field among candidates {}",
                            issue.getKey(), STORY_POINTS_FIELD_CANDIDATES);
                }
                fields.setStoryPoints(storyPoints);
                issue.setFields(fields);
                issues.add(issue);
            }
            return issues;
        } catch (Exception e) {
            log.error("Failed to parse Jira issues response: {}", e.getMessage());
            throw new RuntimeException("Malformed Jira issues response: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts the story-point numeric value from a Jira fields JSON node by
     * iterating the known candidate field names.
     *
     * @return the story-point value, or {@code null} if the field is absent or null in JSON
     */
    Double extractStoryPoints(JsonNode fieldsNode) {
        for (String candidate : STORY_POINTS_FIELD_CANDIDATES) {
            JsonNode node = fieldsNode.path(candidate);
            if (!node.isMissingNode() && !node.isNull() && node.isNumber()) {
                return node.asDouble();
            }
        }
        return null;
    }

    private void assertSuccess(ResponseEntity<String> response, String operation) {
        if (response == null || !response.getStatusCode().is2xxSuccessful()) {
            int code = response != null ? response.getStatusCode().value() : -1;
            log.error("Jira {} returned non-2xx status: {}", operation, code);
            if (code == 429) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Jira rate limit exceeded during " + operation);
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Jira API call failed during " + operation + " (HTTP " + code + ")");
        }
    }

    private String buildUrl(String domain, String path) {
        String host = domain.startsWith("http") ? domain : "https://" + domain;
        return host + path;
    }
}
