package com.seniorapp.controller;

import com.seniorapp.dto.AuthResponse;
import com.seniorapp.service.GitHubService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/github")
public class GitHubAuthController {

    private final GitHubService gitHubService;

    public GitHubAuthController(GitHubService gitHubService) {
        this.gitHubService = gitHubService;
    }

    @PostMapping("/callback")
    public ResponseEntity<?> callback(@RequestBody Map<String, String> request) {
        String code = request.get("code");
        String studentId = request.get("studentId"); // Frontend'den gelecek

        // Not: Burada 'code' ile 'access_token' değişimi yapılmalı. 
        // Şimdilik Başar'ın işini kolaylaştırmak için token üzerinden gidiyoruz.
        AuthResponse response = gitHubService.processGitHubLogin(code, studentId);
        return ResponseEntity.ok(response);
    }
}