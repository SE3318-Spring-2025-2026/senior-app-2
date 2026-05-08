package com.seniorapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Aggressive unit tests for {@link GitHubPrMatcherService}.
 *
 * Edge cases:
 * - No PRs in repo -> empty list
 * - PR branch matches issue key prefix (exact) -> found
 * - PR branch matches case-insensitively -> found
 * - PR branch does not match -> not found
 * - Merged PR detected correctly (merged_at present)
 * - Open PR: merged = false
 * - 401 from GitHub -> 401 exception
 * - 404 from GitHub -> 404 exception
 * - 429 rate limit -> 429 exception
 * - Malformed JSON body -> returns empty list gracefully
 * - Null issueKey -> 400
 * - Blank issueKey -> 400
 * - findFirstMerged -> returns first merged match only
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GitHubPrMatcherService - Unit Tests")
class GitHubPrMatcherServiceTest {

    @Mock private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private GitHubPrMatcherService service;

    @BeforeEach
    void setUp() {
        service = new GitHubPrMatcherService(restTemplate, objectMapper);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // parsePage() – unit tests (no HTTP calls needed)
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("parsePage() – branch matching")
    class ParsePageTests {

        @Test
        @DisplayName("Exact issue key prefix match -> found")
        void exact_prefix_match() {
            String body = prListJson("PROJ-42-add-login", false, "PROJ-42-add-login title", 10);
            List<GitHubPrMatcherService.PrMatch> result = service.parsePage(body, "proj-42");
            assertThat(result).hasSize(1);
            assertThat(result.get(0).headBranch()).isEqualTo("PROJ-42-add-login");
            assertThat(result.get(0).number()).isEqualTo(10);
        }

        @Test
        @DisplayName("Case-insensitive match -> found")
        void case_insensitive_match() {
            String body = prListJson("proj-42-feature", false, "Feature PR", 11);
            List<GitHubPrMatcherService.PrMatch> result = service.parsePage(body, "proj-42");
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Branch does not match -> empty")
        void no_match_returns_empty() {
            String body = prListJson("feature/unrelated-work", false, "Unrelated PR", 5);
            List<GitHubPrMatcherService.PrMatch> result = service.parsePage(body, "proj-42");
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Merged PR: merged_at present -> merged = true")
        void merged_pr_detected() {
            String body = "[{\"number\":1,\"title\":\"t\",\"state\":\"closed\"," +
                    "\"head\":{\"ref\":\"proj-42-feat\"},\"merged_at\":\"2025-01-01T00:00:00Z\"," +
                    "\"html_url\":\"https://github.com/x/y/pull/1\"}]";
            List<GitHubPrMatcherService.PrMatch> result = service.parsePage(body, "proj-42");
            assertThat(result).hasSize(1);
            assertThat(result.get(0).merged()).isTrue();
        }

        @Test
        @DisplayName("Open PR: merged_at = null -> merged = false")
        void open_pr_not_merged() {
            String body = "[{\"number\":2,\"title\":\"t\",\"state\":\"open\"," +
                    "\"head\":{\"ref\":\"proj-42-feat\"},\"merged_at\":null," +
                    "\"html_url\":\"https://github.com/x/y/pull/2\"}]";
            List<GitHubPrMatcherService.PrMatch> result = service.parsePage(body, "proj-42");
            assertThat(result.get(0).merged()).isFalse();
        }

        @Test
        @DisplayName("Empty array -> empty list")
        void empty_array_returns_empty() {
            List<GitHubPrMatcherService.PrMatch> result = service.parsePage("[]", "proj-42");
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Null body -> empty list (no exception)")
        void null_body_returns_empty() {
            assertDoesNotThrow(() -> {
                List<GitHubPrMatcherService.PrMatch> result = service.parsePage(null, "proj-42");
                assertThat(result).isEmpty();
            });
        }

        @Test
        @DisplayName("Malformed JSON -> empty list (no exception)")
        void malformed_json_returns_empty() {
            assertDoesNotThrow(() -> {
                List<GitHubPrMatcherService.PrMatch> result = service.parsePage("{INVALID}", "proj-42");
                assertThat(result).isEmpty();
            });
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // findByBranchPrefix() – HTTP error handling
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("findByBranchPrefix() – HTTP error handling")
    class FindByBranchPrefixTests {

        @Test
        @DisplayName("Blank issueKey -> 400 BAD_REQUEST (no HTTP call)")
        void blank_issue_key_throws_400() {
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.findByBranchPrefix("owner", "repo", "   ", "token"));
            assertThat(ex.getStatusCode().value()).isEqualTo(400);
            verifyNoInteractions(restTemplate);
        }

        @Test
        @DisplayName("Null issueKey -> 400 BAD_REQUEST (no HTTP call)")
        void null_issue_key_throws_400() {
            assertThrows(ResponseStatusException.class,
                    () -> service.findByBranchPrefix("owner", "repo", null, "token"));
            verifyNoInteractions(restTemplate);
        }

        @Test
        @DisplayName("GitHub returns 401 -> throws 401")
        void github_401_throws_unauthorized() {
            when(restTemplate.exchange(any(RequestEntity.class), eq(String.class)))
                    .thenReturn(ResponseEntity.status(401).body("Unauthorized"));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.findByBranchPrefix("owner", "repo", "PROJ-1", "bad-token"));
            assertThat(ex.getStatusCode().value()).isEqualTo(401);
        }

        @Test
        @DisplayName("GitHub returns 404 -> throws 404 (repo not found)")
        void github_404_throws_not_found() {
            when(restTemplate.exchange(any(RequestEntity.class), eq(String.class)))
                    .thenReturn(ResponseEntity.status(404).body("Not Found"));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.findByBranchPrefix("owner", "missing-repo", "PROJ-1", "token"));
            assertThat(ex.getStatusCode().value()).isEqualTo(404);
        }

        @Test
        @DisplayName("GitHub returns 429 -> throws 429 (rate limit)")
        void github_429_throws_rate_limit() {
            when(restTemplate.exchange(any(RequestEntity.class), eq(String.class)))
                    .thenReturn(ResponseEntity.status(429).body("Rate Limited"));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.findByBranchPrefix("owner", "repo", "PROJ-1", "token"));
            assertThat(ex.getStatusCode().value()).isEqualTo(429);
        }

        @Test
        @DisplayName("Empty repo (no PRs) -> returns empty list")
        void empty_repo_returns_empty() {
            when(restTemplate.exchange(any(RequestEntity.class), eq(String.class)))
                    .thenReturn(ResponseEntity.ok("[]"));

            List<GitHubPrMatcherService.PrMatch> result =
                    service.findByBranchPrefix("owner", "repo", "PROJ-42", "token");
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("One matching PR returned on first page")
        void finds_matching_pr() {
            String body = prListJson("PROJ-42-login", false, "Login feature", 7);
            when(restTemplate.exchange(any(RequestEntity.class), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(body));

            List<GitHubPrMatcherService.PrMatch> result =
                    service.findByBranchPrefix("owner", "repo", "PROJ-42", "token");
            assertThat(result).hasSize(1);
            assertThat(result.get(0).number()).isEqualTo(7);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // findFirstMerged()
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("findFirstMerged()")
    class FindFirstMergedTests {

        @Test
        @DisplayName("Open PR only -> returns empty Optional")
        void open_pr_not_returned() {
            String body = "[{\"number\":1,\"title\":\"t\",\"state\":\"open\"," +
                    "\"head\":{\"ref\":\"proj-42-feat\"},\"merged_at\":null," +
                    "\"html_url\":\"https://github.com/x/y/pull/1\"}]";
            when(restTemplate.exchange(any(RequestEntity.class), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(body));

            Optional<GitHubPrMatcherService.PrMatch> result =
                    service.findFirstMerged("o", "r", "PROJ-42", "tok");
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Merged PR -> returns it")
        void merged_pr_returned() {
            String body = "[{\"number\":3,\"title\":\"t\",\"state\":\"closed\"," +
                    "\"head\":{\"ref\":\"proj-42-feat\"},\"merged_at\":\"2025-01-01T00:00:00Z\"," +
                    "\"html_url\":\"https://github.com/x/y/pull/3\"}]";
            when(restTemplate.exchange(any(RequestEntity.class), eq(String.class)))
                    .thenReturn(ResponseEntity.ok(body));

            Optional<GitHubPrMatcherService.PrMatch> result =
                    service.findFirstMerged("o", "r", "PROJ-42", "tok");
            assertThat(result).isPresent();
            assertThat(result.get().number()).isEqualTo(3);
        }
    }

    // ── Helper ─────────────────────────────────────────────────────────────────

    private String prListJson(String branch, boolean merged, String title, int number) {
        String mergedAt = merged ? "\"2025-01-01T00:00:00Z\"" : "null";
        return "[{\"number\":" + number + ",\"title\":\"" + title + "\",\"state\":\"closed\"," +
                "\"head\":{\"ref\":\"" + branch + "\"},\"merged_at\":" + mergedAt + "," +
                "\"html_url\":\"https://github.com/o/r/pull/" + number + "\"}]";
    }
}
