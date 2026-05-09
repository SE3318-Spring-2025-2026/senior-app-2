package com.seniorapp.service;

import com.seniorapp.entity.JiraOAuthState;
import com.seniorapp.entity.User;
import com.seniorapp.repository.JiraOAuthStateRepository;
import com.seniorapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class JiraOAuthService {
    private final JiraOAuthStateRepository jiraOAuthStateRepository;
    private final UserRepository userRepository;
    private final IntegrationCredentialCryptoService cryptoService;

    @Value("${jira.oauth.client.id:}")
    private String jiraClientId;

    @Value("${jira.oauth.client.secret:}")
    private String jiraClientSecret;

    @Value("${jira.oauth.redirect.uri:http://localhost:8080/api/auth/jira/oauth/callback}")
    private String jiraRedirectUri;

    @Value("${jira.oauth.frontend.redirect:http://localhost:5173/panel/profile}")
    private String jiraFrontendRedirect;

    public JiraOAuthService(
            JiraOAuthStateRepository jiraOAuthStateRepository,
            UserRepository userRepository,
            IntegrationCredentialCryptoService cryptoService) {
        this.jiraOAuthStateRepository = jiraOAuthStateRepository;
        this.userRepository = userRepository;
        this.cryptoService = cryptoService;
    }

    public String generateAuthUrl(Long userId) {
        if (userId == null) throw new RuntimeException("Not authenticated");
        assertConfigured();
        String state = UUID.randomUUID().toString();
        jiraOAuthStateRepository.save(new JiraOAuthState(state, userId, LocalDateTime.now()));
        return "https://auth.atlassian.com/authorize"
                + "?audience=api.atlassian.com"
                + "&client_id=" + urlEncode(jiraClientId)
                + "&scope=" + urlEncode(
                        "read:me read:jira-user read:jira-work write:jira-work "
                                + "manage:jira-project manage:jira-configuration "
                                + "read:board-scope:jira-software read:sprint:jira-software write:sprint:jira-software "
                                + "offline_access")
                + "&redirect_uri=" + urlEncode(jiraRedirectUri)
                + "&state=" + urlEncode(state)
                + "&response_type=code"
                + "&prompt=consent";
    }

    public String handleCallback(String code, String state) {
        assertConfigured();
        String safeState = Objects.requireNonNull(state, "state is required");
        JiraOAuthState saved = jiraOAuthStateRepository.findById(safeState)
                .orElseThrow(() -> new RuntimeException("Invalid Jira OAuth state."));
        jiraOAuthStateRepository.delete(Objects.requireNonNull(saved));
        Long userId = Objects.requireNonNull(saved.getUserId(), "state userId is required");
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found."));

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders tokenHeaders = new HttpHeaders();
        tokenHeaders.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> tokenBody = Map.of(
                "grant_type", "authorization_code",
                "client_id", jiraClientId,
                "client_secret", jiraClientSecret,
                "code", code,
                "redirect_uri", jiraRedirectUri
        );
        ResponseEntity<Map<String, Object>> tokenRes = restTemplate.exchange(
                "https://auth.atlassian.com/oauth/token",
                HttpMethod.valueOf("POST"),
                new HttpEntity<>(tokenBody, tokenHeaders),
                new ParameterizedTypeReference<Map<String, Object>>() {});
        Map<String, Object> tokenMap = tokenRes.getBody();
        if (tokenMap == null || !(tokenMap.get("access_token") instanceof String accessToken)) {
            throw new RuntimeException("Jira OAuth token exchange failed.");
        }
        String refreshToken = tokenMap.get("refresh_token") instanceof String s ? s : null;

        HttpHeaders meHeaders = new HttpHeaders();
        meHeaders.setBearerAuth(accessToken);
        RequestEntity<Void> meRequest = RequestEntity.get("https://api.atlassian.com/me")
                .headers(meHeaders)
                .build();
        ResponseEntity<Map<String, Object>> meRes = restTemplate.exchange(
                meRequest,
                new ParameterizedTypeReference<Map<String, Object>>() {});
        Map<String, Object> me = meRes.getBody();

        user.setJiraApiTokenEncrypted(cryptoService.encrypt(accessToken));
        if (refreshToken != null && !refreshToken.isBlank()) {
            user.setJiraRefreshTokenEncrypted(cryptoService.encrypt(refreshToken));
        }
        if (me != null) {
            Object accountId = me.get("account_id");
            Object email = me.get("email");
            Object name = me.get("name");
            user.setJiraAccountId(accountId instanceof String ? (String) accountId : user.getJiraAccountId());
            user.setJiraEmail(email instanceof String ? (String) email : user.getJiraEmail());
            user.setJiraDisplayName(name instanceof String ? (String) name : user.getJiraDisplayName());
        }
        userRepository.save(user);

        return jiraFrontendRedirect + "?jiraOAuth=success";
    }

    private void assertConfigured() {
        if (jiraClientId == null || jiraClientId.isBlank() || jiraClientSecret == null || jiraClientSecret.isBlank()) {
            throw new RuntimeException("Jira OAuth is not configured.");
        }
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
