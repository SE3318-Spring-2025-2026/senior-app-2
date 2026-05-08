package com.seniorapp.service.jira;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seniorapp.dto.jira.JiraDtos;
import com.seniorapp.service.SecureOutboundApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
 * Aggressive unit tests for {@link JiraIntegrationService}.
 *
 * Edge cases covered:
 * - HTTP 429 rate limit propagation
 * - HTTP 401 revoked / invalid token handling
 * - Malformed / empty JSON responses
 * - Missing "Story Points" custom field in all candidate field names
 * - Multiple custom field candidates – first non-null wins
 * - Null assignee in Jira issue fields
 * - Story points from alternate custom field key (customfield_10028)
 * - Domain extraction with and without https prefix
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JiraIntegrationService – Aggressive Unit Tests")
class JiraIntegrationServiceTest {

    @Mock
    private SecureOutboundApiService secureOutboundApiService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private JiraIntegrationService service;

    // Re-create with real ObjectMapper injected via constructor (MockitoExtension uses field injection)
    @BeforeEach
    void setUp() {
        service = new JiraIntegrationService(secureOutboundApiService, objectMapper);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // fetchActiveSprints()
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("fetchActiveSprints()")
    class FetchActiveSprintsTests {

        @Test
        @DisplayName("Returns sprints from valid 200 response")
        void returns_sprints_on_success() {
            String body = """
                    {"values":[{"id":1,"name":"Sprint 1","state":"active","goal":"Ship feature X"}]}
                    """;
            when(secureOutboundApiService.executeJiraApiCall(anyString(), anyString(), isNull(), eq(HttpMethod.GET)))
                    .thenReturn(ResponseEntity.ok(body));

            List<JiraDtos.Sprint> sprints = service.fetchActiveSprints("acme.atlassian.net", "enc:v1:aaa:bbb", 10L);

            assertThat(sprints).hasSize(1);
            assertThat(sprints.get(0).getName()).isEqualTo("Sprint 1");
            assertThat(sprints.get(0).getState()).isEqualTo("active");
        }

        @Test
        @DisplayName("Returns empty list when values array is empty")
        void returns_empty_list_when_no_sprints() {
            when(secureOutboundApiService.executeJiraApiCall(anyString(), anyString(), isNull(), eq(HttpMethod.GET)))
                    .thenReturn(ResponseEntity.ok("{\"values\":[]}"));

            List<JiraDtos.Sprint> sprints = service.fetchActiveSprints("acme.atlassian.net", "enc:v1:aaa:bbb", 10L);

            assertThat(sprints).isEmpty();
        }

        @Test
        @DisplayName("HTTP 429 Rate Limit – throws ResponseStatusException TOO_MANY_REQUESTS")
        void rate_limit_429_propagates() {
            when(secureOutboundApiService.executeJiraApiCall(anyString(), anyString(), isNull(), eq(HttpMethod.GET)))
                    .thenReturn(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("Rate limit exceeded"));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.fetchActiveSprints("acme.atlassian.net", "enc:v1:aaa:bbb", 10L));

            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        }

        @Test
        @DisplayName("HTTP 401 Revoked Token – throws BAD_GATEWAY (non-2xx)")
        void revoked_token_401_throws() {
            when(secureOutboundApiService.executeJiraApiCall(anyString(), anyString(), isNull(), eq(HttpMethod.GET)))
                    .thenReturn(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized"));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.fetchActiveSprints("acme.atlassian.net", "enc:v1:aaa:bbb", 10L));

            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        }

        @Test
        @DisplayName("Malformed JSON response – throws RuntimeException")
        void malformed_json_throws_runtime_exception() {
            when(secureOutboundApiService.executeJiraApiCall(anyString(), anyString(), isNull(), eq(HttpMethod.GET)))
                    .thenReturn(ResponseEntity.ok("NOT_VALID_JSON{{{{"));

            assertThrows(RuntimeException.class,
                    () -> service.fetchActiveSprints("acme.atlassian.net", "enc:v1:aaa:bbb", 10L));
        }

        @Test
        @DisplayName("URL includes https when domain starts with https")
        void url_built_correctly_with_https_prefix() {
            when(secureOutboundApiService.executeJiraApiCall(anyString(), contains("https://acme.atlassian.net"), isNull(), eq(HttpMethod.GET)))
                    .thenReturn(ResponseEntity.ok("{\"values\":[]}"));

            service.fetchActiveSprints("https://acme.atlassian.net", "enc:v1:aaa:bbb", 5L);

            verify(secureOutboundApiService).executeJiraApiCall(
                    anyString(), contains("https://acme.atlassian.net"), isNull(), eq(HttpMethod.GET));
        }

        @Test
        @DisplayName("URL does NOT double-add https when domain is plain host")
        void url_built_correctly_without_double_https() {
            when(secureOutboundApiService.executeJiraApiCall(anyString(), contains("https://acme"), isNull(), eq(HttpMethod.GET)))
                    .thenReturn(ResponseEntity.ok("{\"values\":[]}"));

            service.fetchActiveSprints("acme.atlassian.net", "enc:v1:aaa:bbb", 5L);

            verify(secureOutboundApiService).executeJiraApiCall(
                    anyString(), startsWith("https://acme"), isNull(), eq(HttpMethod.GET));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // fetchIssuesByJql() + parseIssuesWithStoryPoints()
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("fetchIssuesByJql() – Story Point Custom Field Handling")
    class FetchIssuesByJqlTests {

        @Test
        @DisplayName("Parses story points from 'story_points' field")
        void parses_story_points_from_story_points_field() {
            String body = issueJson("story_points", 8.0);
            when(secureOutboundApiService.executeJiraApiCall(anyString(), anyString(), isNull(), eq(HttpMethod.GET)))
                    .thenReturn(ResponseEntity.ok(body));

            List<JiraDtos.Issue> issues = service.fetchIssuesByJql("acme.atlassian.net", "enc:v1:aaa:bbb", "project=X");

            assertThat(issues).hasSize(1);
            assertThat(issues.get(0).getFields().getStoryPoints()).isEqualTo(8.0);
        }

        @Test
        @DisplayName("Parses story points from 'customfield_10016' when story_points absent")
        void parses_story_points_from_customfield_10016() {
            String body = issueJson("customfield_10016", 5.0);
            when(secureOutboundApiService.executeJiraApiCall(anyString(), anyString(), isNull(), eq(HttpMethod.GET)))
                    .thenReturn(ResponseEntity.ok(body));

            List<JiraDtos.Issue> issues = service.fetchIssuesByJql("acme.atlassian.net", "enc:v1:aaa:bbb", "project=X");

            assertThat(issues.get(0).getFields().getStoryPoints()).isEqualTo(5.0);
        }

        @Test
        @DisplayName("Parses story points from 'customfield_10028' (third candidate)")
        void parses_story_points_from_customfield_10028() {
            String body = issueJson("customfield_10028", 13.0);
            when(secureOutboundApiService.executeJiraApiCall(anyString(), anyString(), isNull(), eq(HttpMethod.GET)))
                    .thenReturn(ResponseEntity.ok(body));

            List<JiraDtos.Issue> issues = service.fetchIssuesByJql("acme.atlassian.net", "enc:v1:aaa:bbb", "project=X");

            assertThat(issues.get(0).getFields().getStoryPoints()).isEqualTo(13.0);
        }

        @Test
        @DisplayName("Missing story point custom field → storyPoints is null (no exception)")
        void missing_story_points_field_yields_null() {
            String body = """
                    {"issues":[{"id":"10","key":"PROJ-1","fields":{
                        "summary":"Fix bug","description":null,
                        "status":{"name":"In Progress"},
                        "priority":{"name":"High"},
                        "assignee":{"displayName":"Alice","emailAddress":"alice@test.com"}
                    }}]}
                    """;
            when(secureOutboundApiService.executeJiraApiCall(anyString(), anyString(), isNull(), eq(HttpMethod.GET)))
                    .thenReturn(ResponseEntity.ok(body));

            List<JiraDtos.Issue> issues = service.fetchIssuesByJql("acme.atlassian.net", "enc:v1:aaa:bbb", "project=X");

            assertThat(issues).hasSize(1);
            assertThat(issues.get(0).getFields().getStoryPoints()).isNull(); // graceful missing field
        }

        @Test
        @DisplayName("Null assignee in Jira issue – does not throw, assignee is null")
        void null_assignee_does_not_throw() {
            String body = """
                    {"issues":[{"id":"10","key":"PROJ-1","fields":{
                        "summary":"Unassigned task","description":null,
                        "status":{"name":"To Do"},
                        "priority":{"name":"Medium"},
                        "assignee":null,
                        "story_points":3.0
                    }}]}
                    """;
            when(secureOutboundApiService.executeJiraApiCall(anyString(), anyString(), isNull(), eq(HttpMethod.GET)))
                    .thenReturn(ResponseEntity.ok(body));

            List<JiraDtos.Issue> issues = service.fetchIssuesByJql("acme.atlassian.net", "enc:v1:aaa:bbb", "project=X");

            assertThat(issues.get(0).getFields().getAssignee()).isNull();
            assertThat(issues.get(0).getFields().getStoryPoints()).isEqualTo(3.0);
        }

        @Test
        @DisplayName("Rate limit 429 on issue search – throws TOO_MANY_REQUESTS")
        void rate_limit_on_issue_search_throws() {
            when(secureOutboundApiService.executeJiraApiCall(anyString(), anyString(), isNull(), eq(HttpMethod.GET)))
                    .thenReturn(ResponseEntity.status(429).body(""));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.fetchIssuesByJql("acme.atlassian.net", "enc:v1:aaa:bbb", "project=X"));

            assertThat(ex.getStatusCode().value()).isEqualTo(429);
        }

        @Test
        @DisplayName("Empty body – returns empty list without exception")
        void empty_body_returns_empty_list() {
            // Call the parsing method directly – no HTTP mock needed
            List<JiraDtos.Issue> issues = service.parseIssuesWithStoryPoints("");
            assertThat(issues).isEmpty();
        }

        @Test
        @DisplayName("Story points field is JSON null → storyPoints is null")
        void json_null_story_points_yields_null() {
            String body = """
                    {"issues":[{"id":"1","key":"P-1","fields":{
                        "summary":"S","description":null,
                        "status":{"name":"Done"},
                        "priority":{"name":"Low"},
                        "assignee":null,
                        "story_points":null
                    }}]}
                    """;
            when(secureOutboundApiService.executeJiraApiCall(anyString(), anyString(), isNull(), eq(HttpMethod.GET)))
                    .thenReturn(ResponseEntity.ok(body));

            List<JiraDtos.Issue> issues = service.fetchIssuesByJql("acme.atlassian.net", "enc:v1:aaa:bbb", "project=X");
            assertThat(issues.get(0).getFields().getStoryPoints()).isNull();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // buildOpenSprintJql()
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("buildOpenSprintJql()")
    class JqlBuilderTests {

        @Test
        @DisplayName("JQL contains project key and openSprints()")
        void jql_contains_project_key() {
            String jql = service.buildOpenSprintJql("MYAPP");
            assertThat(jql).contains("MYAPP").contains("openSprints()");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // extractStoryPoints() – unit tests
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("extractStoryPoints() – internal unit")
    class ExtractStoryPointsTests {

        @Test
        @DisplayName("First matching candidate field returns its value")
        void first_matching_candidate_wins() throws Exception {
            String json = "{\"story_points\":5.0,\"customfield_10016\":9.0}";
            var node = objectMapper.readTree(json);
            assertThat(service.extractStoryPoints(node)).isEqualTo(5.0);
        }

        @Test
        @DisplayName("Falls back to second candidate when first is absent")
        void falls_back_to_second_candidate() throws Exception {
            String json = "{\"customfield_10016\":7.0}";
            var node = objectMapper.readTree(json);
            assertThat(service.extractStoryPoints(node)).isEqualTo(7.0);
        }

        @Test
        @DisplayName("Returns null when no candidate field is present")
        void returns_null_when_no_candidate_present() throws Exception {
            String json = "{\"summary\":\"Some task\"}";
            var node = objectMapper.readTree(json);
            assertThat(service.extractStoryPoints(node)).isNull();
        }

        @Test
        @DisplayName("Returns null when all candidates are JSON null")
        void returns_null_when_all_candidates_are_json_null() throws Exception {
            String json = "{\"story_points\":null,\"customfield_10016\":null}";
            var node = objectMapper.readTree(json);
            assertThat(service.extractStoryPoints(node)).isNull();
        }
    }

    // ── Helper ─────────────────────────────────────────────────────────────────

    private String issueJson(String storyPointField, double points) {
        return """
                {"issues":[{"id":"10","key":"PROJ-1","fields":{
                    "summary":"Implement login","description":"Full auth flow",
                    "status":{"name":"In Progress"},
                    "priority":{"name":"High"},
                    "assignee":{"displayName":"Bob","emailAddress":"bob@test.com"},
                    "%s":%s
                }}]}
                """.formatted(storyPointField, points);
    }
}
