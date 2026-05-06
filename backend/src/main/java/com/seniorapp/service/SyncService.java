package com.seniorapp.service;

import com.seniorapp.dto.SyncRequest;
import com.seniorapp.dto.SyncResponse;
import com.seniorapp.entity.AuditLog;
import com.seniorapp.repository.AuditLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
public class SyncService {

    private final GitHubService gitHubService;
    private final AuditLogRepository auditLogRepository;
    private final LogService logService;
    private final TaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();

    public SyncService(GitHubService gitHubService,
                       AuditLogRepository auditLogRepository,
                       LogService logService) {
        this.gitHubService = gitHubService;
        this.auditLogRepository = auditLogRepository;
        this.logService = logService;
    }

    public SyncResponse initiateSync(SyncRequest request, Long userId, String userRole) throws TimeoutException {
        if (request == null) {
            throw new IllegalArgumentException("Sync request cannot be null");
        }

        validateRequest(request);

        if (request.getSource() == SyncRequest.SyncSource.GITHUB || request.getSource() == SyncRequest.SyncSource.BOTH) {
            String pat = request.getIntegrationTokens() != null ? request.getIntegrationTokens().getPat() : null;
            gitHubService.fetchGitHubSyncPayload(pat);
        }

        String jobId = UUID.randomUUID().toString();
        SyncResponse response = SyncResponse.builder()
                .jobId(jobId)
                .status("accepted")
                .createdAt(LocalDateTime.now())
                .message("Sync job initiated for group: " + request.getGroupId())
                .build();

        saveAuditLog(userId, userRole,
                "ingestion",
                "sync_initiated",
                "success",
                "info",
                "Sync initiated for groupId=" + request.getGroupId() + " source=" + request.getSource());

        taskExecutor.execute(() -> performSyncAsync(request, userId, userRole, jobId));
        return response;
    }

    @Async
    public void performSyncAsync(SyncRequest request, Long userId, String userRole, String jobId) {
        try {
            log.info("Starting async sync for jobId={} groupId={}", jobId, request.getGroupId());

            if (request.getSource() == SyncRequest.SyncSource.GITHUB || request.getSource() == SyncRequest.SyncSource.BOTH) {
                syncGitHub(request, userId, userRole, jobId);
            }

            if (request.getSource() == SyncRequest.SyncSource.JIRA || request.getSource() == SyncRequest.SyncSource.BOTH) {
                syncJira(request, userId, userRole, jobId);
            }

            saveAuditLog(userId, userRole,
                    "ingestion",
                    "sync_completed",
                    "success",
                    "info",
                    "Data sync completed successfully for jobId=" + jobId);

            log.info("Async sync completed for jobId={}", jobId);
        } catch (Exception ex) {
            log.error("Async sync failed for jobId={}", jobId, ex);
            saveAuditLog(userId, userRole,
                    "ingestion",
                    "sync_failed",
                    "failed",
                    "high",
                    "Data sync failed: " + ex.getMessage() + " (jobId=" + jobId + ")");
        }
    }

    private void validateRequest(SyncRequest request) {
        if (request.getSource() == null) {
            throw new IllegalArgumentException("Sync source is required");
        }
        if (request.getGroupId() == null || request.getGroupId().isBlank()) {
            throw new IllegalArgumentException("Group ID is required");
        }
        if ((request.getSource() == SyncRequest.SyncSource.GITHUB || request.getSource() == SyncRequest.SyncSource.BOTH)
                && (request.getIntegrationTokens() == null || request.getIntegrationTokens().getPat() == null || request.getIntegrationTokens().getPat().isBlank())) {
            throw new IllegalArgumentException("GitHub PAT is required for GitHub sync");
        }
    }

    private void syncGitHub(SyncRequest request, Long userId, String userRole, String jobId) {
        try {
            log.debug("Syncing GitHub data for jobId={}", jobId);
            String pat = request.getIntegrationTokens() != null ? request.getIntegrationTokens().getPat() : null;
            String payload = gitHubService.fetchGitHubSyncPayload(pat);
            log.debug("GitHub sync payload for jobId={}: {}", jobId, payload);
        } catch (TimeoutException ex) {
            throw new RuntimeException("GitHub sync timed out", ex);
        } catch (Exception ex) {
            throw new RuntimeException("GitHub sync failed", ex);
        }
    }

    private void syncJira(SyncRequest request, Long userId, String userRole, String jobId) {
        log.debug("Syncing Jira data for jobId={}", jobId);
        saveAuditLog(userId, userRole,
                "ingestion",
                "jira_sync_placeholder",
                "success",
                "info",
                "Jira sync placeholder completed for jobId=" + jobId);
    }

    private void saveAuditLog(Long userId,
                              String userRole,
                              String module,
                              String action,
                              String status,
                              String severity,
                              String message) {
        AuditLog auditLog = new AuditLog();
        auditLog.setUserId(userId);
        auditLog.setUserRole(userRole);
        auditLog.setModule(module);
        auditLog.setAction(action);
        auditLog.setStatus(status);
        auditLog.setSeverity(severity);
        auditLog.setMessage(message);
        auditLogRepository.save(auditLog);
    }
}
