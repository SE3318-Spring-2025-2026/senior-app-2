package com.seniorapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seniorapp.dto.SyncRequest;
import com.seniorapp.dto.SyncResponse;
import com.seniorapp.entity.AuditLog;
import com.seniorapp.entity.Role;
import com.seniorapp.entity.User;
import com.seniorapp.repository.AuditLogRepository;
import com.seniorapp.repository.UserGroupRepository;
import com.seniorapp.repository.UserRepository;
import com.seniorapp.service.GitHubService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "github.client.id=test-github-client-id",
        "github.client.secret=test-github-secret",
        "app.jwt.secret=test-jwt-secret-key-must-be-at-least-32-chars-long"
})
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MYSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("Issue #125: Data Sync Integration Tests")
class Issue125SyncIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UserGroupRepository userGroupRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private GitHubService gitHubService;

    private User testUser;
    private static final String SYNC_ENDPOINT = "/api/ingestion/sync";
    private static final String GROUP_ID = "test-group-123";
    private static final String GITHUB_PAT = "ghp_test_token_1234567890";

    @BeforeEach
    void setUp() {
        userGroupRepository.deleteAll();
        auditLogRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setEmail("testcoordinator@example.com");
        testUser.setFullName("Test Coordinator");
        testUser.setRole(Role.COORDINATOR);
        testUser.setEnabled(true);
        testUser.setGithubUsername("test-github-user");
        testUser = userRepository.save(testUser);
    }

    @Test
    @WithMockUser(username = "testcoordinator@example.com", roles = "COORDINATOR")
    @DisplayName("SUCCESS: POST /api/ingestion/sync returns 202 and persists AuditLog")
    void shouldReturnAcceptedAndPersistAuditLogWhenSyncRequestIsValid() throws Exception {
        when(gitHubService.fetchGitHubSyncPayload(eq(GITHUB_PAT)))
                .thenReturn("{\"repositories\": []}");

        SyncRequest.IntegrationTokens tokens = SyncRequest.IntegrationTokens.builder()
                .pat(GITHUB_PAT)
                .build();

        SyncRequest request = SyncRequest.builder()
                .source(SyncRequest.SyncSource.GITHUB)
                .groupId(GROUP_ID)
                .integrationTokens(tokens)
                .build();

        mockMvc.perform(post(SYNC_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("accepted"))
                .andExpect(jsonPath("$.jobId").isNotEmpty());

        assertThat(auditLogRepository.countByModule("ingestion")).isGreaterThan(0);

        AuditLog auditLog = auditLogRepository.findAll().stream()
                .filter(log -> "ingestion".equals(log.getModule()))
                .filter(log -> "sync_initiated".equals(log.getAction()))
                .findFirst()
                .orElseThrow();

        assertThat(auditLog.getMessage()).contains(GROUP_ID);
        assertThat(auditLog.getStatus()).isEqualTo("success");
    }

    @Test
    @WithMockUser(username = "testcoordinator@example.com", roles = "COORDINATOR")
    @DisplayName("VALIDATION FAILURE: POST /api/ingestion/sync missing groupId returns 400")
    void shouldReturnBadRequestWhenGroupIdIsMissing() throws Exception {
        String invalidPayload = "{\"source\": \"GITHUB\", \"integrationTokens\": {\"pat\": \"" + GITHUB_PAT + "\"}}";

        mockMvc.perform(post(SYNC_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());

        assertThat(auditLogRepository.count()).isEqualTo(0);
    }

    @Test
    @WithMockUser(username = "testcoordinator@example.com", roles = "COORDINATOR")
    @DisplayName("TIMEOUT FAILURE: GitHubService timeout returns 504 Gateway Timeout")
    void shouldReturnGatewayTimeoutWhenGitHubServiceTimesOut() throws Exception {
        when(gitHubService.fetchGitHubSyncPayload(eq(GITHUB_PAT)))
                .thenThrow(new TimeoutException("30 second timeout"));

        SyncRequest.IntegrationTokens tokens = SyncRequest.IntegrationTokens.builder()
                .pat(GITHUB_PAT)
                .build();

        SyncRequest request = SyncRequest.builder()
                .source(SyncRequest.SyncSource.GITHUB)
                .groupId(GROUP_ID)
                .integrationTokens(tokens)
                .build();

        mockMvc.perform(post(SYNC_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.status").value("failed"));

        assertThat(auditLogRepository.countByModule("ingestion")).isGreaterThan(0);
        assertThat(auditLogRepository.findAll().stream()
                        .anyMatch(log -> "sync_timeout".equals(log.getAction()) || log.getMessage().contains("timed out")))
                .isTrue();
    }
}
