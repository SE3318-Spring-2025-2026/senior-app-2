package com.seniorapp.controller;

import com.seniorapp.dto.AuthResponse;
import com.seniorapp.service.GitHubService;
import com.seniorapp.service.LogService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/github")
public class GitHubAuthController {

    private static final Logger log = LoggerFactory.getLogger(GitHubAuthController.class);

    private final GitHubService gitHubService;
    private final LogService logService;

    public GitHubAuthController(GitHubService gitHubService, LogService logService) {
        this.gitHubService = gitHubService;
        this.logService = logService;
    }

    @PostMapping("/callback")
    public ResponseEntity<?> callback(@RequestBody Map<String, String> request,
                                      HttpServletRequest httpRequest) {
        String code = request.get("code");
        String studentId = request.get("studentId"); // Frontend'den gelecek

        log.debug("Legacy GitHub callback POST invoked");
        try {
            AuthResponse response = gitHubService.processGitHubLogin(code, studentId);
            logService.saveAuthLog(
                    response.getUser().getId(),
                    response.getUser().getRole(),
                    "github_legacy_api_callback",
                    "success",
                    "Legacy /api/auth/github/callback completed",
                    httpRequest
            );
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.warn("Legacy GitHub callback failed: {}", e.getMessage());
            try {
                logService.saveAuthLog(
                        null,
                        null,
                        "github_legacy_api_callback",
                        "failed",
                        e.getMessage() != null ? e.getMessage() : "GitHub legacy callback failed",
                        httpRequest
                );
            } catch (Exception auditEx) {
                log.warn("Could not persist failed GitHub legacy audit", auditEx);
            }
            throw e;
        }
    }
}