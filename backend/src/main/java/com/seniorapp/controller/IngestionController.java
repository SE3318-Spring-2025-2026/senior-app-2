package com.seniorapp.controller;

import com.seniorapp.dto.SyncRequest;
import com.seniorapp.dto.SyncResponse;
import com.seniorapp.entity.User;
import com.seniorapp.repository.UserRepository;
import com.seniorapp.service.LogService;
import com.seniorapp.service.SyncService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.concurrent.TimeoutException;

@RestController
@RequestMapping("/api/ingestion")
@CrossOrigin(origins = "*")
@Slf4j
public class IngestionController {

    private final SyncService syncService;
    private final LogService logService;
    private final UserRepository userRepository;

    public IngestionController(SyncService syncService,
                               LogService logService,
                               UserRepository userRepository) {
        this.syncService = syncService;
        this.logService = logService;
        this.userRepository = userRepository;
    }

    @PostMapping("/sync")
    public ResponseEntity<SyncResponse> initiateSync(
            @Valid @RequestBody SyncRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {

        try {
            String username = authentication.getName();
            User currentUser = userRepository.findByEmail(username)
                    .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));

            log.info("Sync initiated for groupId={} source={} by userId={}", 
                    request.getGroupId(), request.getSource(), currentUser.getId());

            SyncResponse response = syncService.initiateSync(request, currentUser.getId(), currentUser.getRole().name());

            logService.saveAuthLog(
                    currentUser.getId(),
                    currentUser.getRole().name(),
                    "sync_initiated",
                    "success",
                    "Data sync initiated for groupId=" + request.getGroupId() + ", source=" + request.getSource(),
                    httpRequest
            );

            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);

        } catch (TimeoutException ex) {
            log.warn("Sync initiation timed out", ex);
            logService.saveErrorLog(
                    null,
                    null,
                    "ingestion",
                    "sync_timeout",
                    "Sync initiation timed out: " + ex.getMessage(),
                    httpRequest
            );
            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                    .body(SyncResponse.builder()
                            .status("failed")
                            .message("Sync request timed out: " + ex.getMessage())
                            .build());
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid sync request", ex);
            logService.saveErrorLog(
                    null,
                    null,
                    "ingestion",
                    "sync_bad_request",
                    ex.getMessage(),
                    httpRequest
            );
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(SyncResponse.builder()
                            .status("failed")
                            .message(ex.getMessage())
                            .build());
        } catch (Exception ex) {
            log.error("Failed to initiate sync", ex);
            logService.saveErrorLog(
                    null,
                    null,
                    "ingestion",
                    "sync_error",
                    "Failed to initiate sync: " + ex.getMessage(),
                    httpRequest
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(SyncResponse.builder()
                            .status("failed")
                            .message("Failed to initiate sync: " + ex.getMessage())
                            .build());
        }
    }

    @GetMapping("/status/{jobId}")
    public ResponseEntity<SyncResponse> checkSyncStatus(@PathVariable String jobId) {
        SyncResponse response = SyncResponse.builder()
                .jobId(jobId)
                .status("running")
                .message("Sync job is in progress")
                .build();
        return ResponseEntity.ok(response);
    }
}
