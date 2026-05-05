package com.seniorapp.controller;

import com.seniorapp.entity.User;
import com.seniorapp.service.GitHubProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/github")
public class GitHubProfileController {

    private final GitHubProfileService gitHubProfileService;

    public GitHubProfileController(GitHubProfileService gitHubProfileService) {
        this.gitHubProfileService = gitHubProfileService;
    }

    @GetMapping("/profile")
    @PreAuthorize("hasAnyRole('STUDENT', 'COORDINATOR', 'PROFESSOR', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getGitHubProfile(
            @AuthenticationPrincipal User principal,
            @RequestParam(required = false) Long userId
    ) {
        // Students can only view their own profile, others can view any
        if (principal.getRole().name().equals("STUDENT") && userId != null && !userId.equals(principal.getId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Students can only view their own profile"));
        }

        Long targetUserId = userId != null ? userId : principal.getId();
        return ResponseEntity.ok(gitHubProfileService.getGitHubProfile(targetUserId));
    }

    @GetMapping("/repositories")
    @PreAuthorize("hasAnyRole('STUDENT', 'COORDINATOR', 'PROFESSOR', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getGitHubRepositories(
            @AuthenticationPrincipal User principal,
            @RequestParam(required = false) Long userId
    ) {
        if (principal.getRole().name().equals("STUDENT") && userId != null && !userId.equals(principal.getId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Students can only view their own repositories"));
        }

        Long targetUserId = userId != null ? userId : principal.getId();
        return ResponseEntity.ok(gitHubProfileService.getGitHubRepositories(targetUserId));
    }

    @GetMapping("/activity")
    @PreAuthorize("hasAnyRole('STUDENT', 'COORDINATOR', 'PROFESSOR', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getGitHubActivity(
            @AuthenticationPrincipal User principal,
            @RequestParam(required = false) Long userId
    ) {
        if (principal.getRole().name().equals("STUDENT") && userId != null && !userId.equals(principal.getId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Students can only view their own activity"));
        }

        Long targetUserId = userId != null ? userId : principal.getId();
        return ResponseEntity.ok(gitHubProfileService.getGitHubActivity(targetUserId));
    }
}
