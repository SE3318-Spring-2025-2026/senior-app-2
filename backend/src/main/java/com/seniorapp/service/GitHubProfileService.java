package com.seniorapp.service;

import com.seniorapp.entity.User;
import com.seniorapp.entity.UserGroup;
import com.seniorapp.entity.UserGroupMember;
import com.seniorapp.entity.GroupInviteStatus;
import com.seniorapp.repository.UserGroupMemberRepository;
import com.seniorapp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class GitHubProfileService {

    private static final Logger log = LoggerFactory.getLogger(GitHubProfileService.class);

    private final UserRepository userRepository;
    private final UserGroupMemberRepository userGroupMemberRepository;
    private final SecureOutboundApiService outboundApiService;
    private final RestTemplate restTemplate = new RestTemplate();

    public GitHubProfileService(
            UserRepository userRepository,
            UserGroupMemberRepository userGroupMemberRepository,
            SecureOutboundApiService outboundApiService
    ) {
        this.userRepository = userRepository;
        this.userGroupMemberRepository = userGroupMemberRepository;
        this.outboundApiService = outboundApiService;
    }

    /**
     * Try to get encrypted PAT token from user's group.
     * Returns null if user has no group or group has no token.
     */
    private String getGroupToken(User user) {
        Optional<UserGroupMember> membership = userGroupMemberRepository
                .findByUserAndStatus(user, GroupInviteStatus.ACCEPTED);
        if (membership.isEmpty()) return null;
        String token = membership.get().getGroup().getGithubPatEncrypted();
        return (token == null || token.isBlank()) ? null : token;
    }

    /**
     * Call GitHub API. Uses group PAT token if available, otherwise unauthenticated.
     */
    private ResponseEntity<String> callGitHubApi(String url, String encryptedToken) {
        if (encryptedToken != null) {
            try {
                return outboundApiService.executeGitHubApiCall(encryptedToken, url, null, HttpMethod.GET);
            } catch (Exception e) {
                log.warn("Authenticated GitHub API call failed, falling back to unauthenticated: {}", e.getMessage());
            }
        }
        // Unauthenticated fallback (public data only, lower rate limit)
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        headers.set("User-Agent", "SeniorApp");
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        return restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
    }

    public Map<String, Object> getGitHubProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getGithubUsername() == null) {
            return Map.of("status", "not_linked", "message", "GitHub account not linked");
        }

        String encryptedToken = getGroupToken(user);

        try {
            String apiUrl = "https://api.github.com/users/" + user.getGithubUsername();
            ResponseEntity<String> response = callGitHubApi(apiUrl, encryptedToken);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("githubUsername", user.getGithubUsername());
            result.put("githubId", user.getGithubId());
            result.put("profileData", response.getBody());
            return result;

        } catch (Exception e) {
            log.error("Failed to fetch GitHub profile for user {}", userId, e);
            return Map.of("status", "error",
                    "message", "Failed to fetch GitHub profile: " + e.getMessage(),
                    "githubUsername", user.getGithubUsername());
        }
    }

    public Map<String, Object> getGitHubRepositories(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getGithubUsername() == null) {
            return Map.of("status", "not_linked", "message", "GitHub account not linked");
        }

        String encryptedToken = getGroupToken(user);

        try {
            String apiUrl = "https://api.github.com/users/" + user.getGithubUsername() + "/repos?sort=updated&per_page=10";
            ResponseEntity<String> response = callGitHubApi(apiUrl, encryptedToken);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("githubUsername", user.getGithubUsername());
            result.put("repositories", response.getBody());
            return result;

        } catch (Exception e) {
            log.error("Failed to fetch GitHub repositories for user {}", userId, e);
            return Map.of("status", "error",
                    "message", "Failed to fetch repositories: " + e.getMessage(),
                    "githubUsername", user.getGithubUsername());
        }
    }

    public Map<String, Object> getGitHubActivity(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getGithubUsername() == null) {
            return Map.of("status", "not_linked", "message", "GitHub account not linked");
        }

        String encryptedToken = getGroupToken(user);

        try {
            String apiUrl = "https://api.github.com/users/" + user.getGithubUsername() + "/events/public?per_page=10";
            ResponseEntity<String> response = callGitHubApi(apiUrl, encryptedToken);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "success");
            result.put("githubUsername", user.getGithubUsername());
            result.put("activity", response.getBody());
            return result;

        } catch (Exception e) {
            log.error("Failed to fetch GitHub activity for user {}", userId, e);
            return Map.of("status", "error",
                    "message", "Failed to fetch activity: " + e.getMessage(),
                    "githubUsername", user.getGithubUsername());
        }
    }
}
