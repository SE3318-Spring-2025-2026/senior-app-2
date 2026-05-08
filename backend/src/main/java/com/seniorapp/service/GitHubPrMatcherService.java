package com.seniorapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Finds GitHub Pull Requests whose head branch starts with a Jira issue key.
 *
 * <p>Spec requirement: "find branches starting with the specified issue key
 * and obtain the related PR to check if it has been merged."
 *
 * <p>Uses the GitHub REST API:
 * {@code GET /repos/{owner}/{repo}/pulls?state=all&per_page=100&page=N}
 *
 * <p>Authentication is handled via a PAT (Personal Access Token) passed as a
 * Bearer token. The caller is responsible for providing a decrypted token.
 */
@Service
public class GitHubPrMatcherService {

    private static final Logger log = LoggerFactory.getLogger(GitHubPrMatcherService.class);
    private static final String GITHUB_API = "https://api.github.com";
    private static final int MAX_PAGES = 10; // guard against enormous repos

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GitHubPrMatcherService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Represents a matching pull request.
     */
    public record PrMatch(
            int number,
            String title,
            String headBranch,
            String state,          // "open" | "closed"
            boolean merged,
            String htmlUrl
    ) {}

    /**
     * Finds all PRs in a repository whose head branch starts with the given Jira issue key
     * (case-insensitive). Searches both open and closed PRs.
     *
     * @param owner      GitHub organization or user name
     * @param repo       repository name
     * @param issueKey   Jira issue key, e.g. "PROJ-42"
     * @param githubPat  decrypted GitHub Personal Access Token (must have repo scope)
     * @return list of matching PRs (may be empty)
     * @throws ResponseStatusException 401 if token is invalid; 404 if repo not found; 429 on rate limit
     */
    public List<PrMatch> findByBranchPrefix(String owner, String repo, String issueKey, String githubPat) {
        if (issueKey == null || issueKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "issueKey must not be blank");
        }
        String prefix = issueKey.toLowerCase().strip();

        List<PrMatch> matches = new ArrayList<>();
        int page = 1;

        while (page <= MAX_PAGES) {
            String url = buildPrListUrl(owner, repo, page);
            ResponseEntity<String> resp = fetchPage(url, githubPat);

            if (resp.getStatusCode().value() == 401) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "GitHub PAT is invalid or expired for repo " + owner + "/" + repo);
            }
            if (resp.getStatusCode().value() == 404) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "GitHub repository not found: " + owner + "/" + repo);
            }
            if (resp.getStatusCode().value() == 429) {
                log.warn("[GitHubPrMatcher] Rate limited by GitHub API for {}/{}", owner, repo);
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "GitHub API rate limit hit while searching PRs for " + owner + "/" + repo);
            }

            List<PrMatch> pageMatches = parsePage(resp.getBody(), prefix);
            if (pageMatches.isEmpty() && page > 1) break; // stop pagination when empty page
            matches.addAll(pageMatches);

            // GitHub returns fewer than 100 results on the last page
            if (parsePageSize(resp.getBody()) < 100) break;
            page++;
        }

        return matches;
    }

    /**
     * Convenience method: returns the first merged PR matching the issue key, if any.
     */
    public Optional<PrMatch> findFirstMerged(String owner, String repo, String issueKey, String githubPat) {
        return findByBranchPrefix(owner, repo, issueKey, githubPat)
                .stream()
                .filter(PrMatch::merged)
                .findFirst();
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    private String buildPrListUrl(String owner, String repo, int page) {
        return GITHUB_API + "/repos/" + owner + "/" + repo
                + "/pulls?state=all&per_page=100&page=" + page;
    }

    private ResponseEntity<String> fetchPage(String url, String githubPat) {
        try {
            RequestEntity<Void> req = RequestEntity
                    .get(URI.create(url))
                    .header("Authorization", "Bearer " + githubPat)
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .build();
            return restTemplate.exchange(req, String.class);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // Return a synthetic response so the caller can inspect the status
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        }
    }

    List<PrMatch> parsePage(String body, String prefix) {
        if (body == null || body.isBlank()) return List.of();
        try {
            JsonNode root = objectMapper.readTree(body);
            if (!root.isArray()) return List.of();

            List<PrMatch> result = new ArrayList<>();
            for (JsonNode pr : root) {
                String headBranch = pr.at("/head/ref").asText("");
                if (headBranch.toLowerCase().startsWith(prefix)) {
                    result.add(new PrMatch(
                            pr.path("number").asInt(),
                            pr.path("title").asText(""),
                            headBranch,
                            pr.path("state").asText(""),
                            !pr.path("merged_at").isNull() && !pr.path("merged_at").isMissingNode(),
                            pr.path("html_url").asText("")
                    ));
                }
            }
            return result;
        } catch (Exception e) {
            log.error("[GitHubPrMatcher] Failed to parse PR list response: {}", e.getMessage());
            return List.of();
        }
    }

    private int parsePageSize(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            return root.isArray() ? root.size() : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}
