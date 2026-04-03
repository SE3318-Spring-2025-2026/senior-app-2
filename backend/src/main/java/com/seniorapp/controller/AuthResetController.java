package com.seniorapp.controller;

import com.seniorapp.service.PasswordResetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth/reset-password")
public class AuthResetController {

    private final PasswordResetService passwordResetService;

    public AuthResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    // Triggered by Admin from the dashboard
    @PostMapping("/admin")
    public ResponseEntity<?> resetByAdmin(@RequestBody Map<String, Long> payload) {
        Long accountId = payload.get("accountId");
        passwordResetService.generateAndSendTokenByUserId(accountId);
        return ResponseEntity.ok(Map.of("message", "Reset link sent successfully."));
    }

    // Forgot password triggered via Email
    @PostMapping("/forgot")
    public ResponseEntity<?> resetByEmail(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        passwordResetService.generateAndSendTokenByEmail(email);
        return ResponseEntity.ok(Map.of("message", "If the email exists, a reset link has been sent."));
    }

    // Check token validity
    @GetMapping("/check-token-validity")
    public ResponseEntity<?> checkTokenValidity(@RequestParam String token) {
        boolean isValid = passwordResetService.isTokenValid(token);
        return ResponseEntity.ok(Map.of("valid", isValid));
    }

    // Save the new password
    @PostMapping("/new-password")
    public ResponseEntity<?> setNewPassword(@RequestBody Map<String, String> payload) {
        String token = payload.get("token");
        String newPassword = payload.get("newPassword");
        passwordResetService.updatePassword(token, newPassword);
        return ResponseEntity.ok(Map.of("message", "Password updated successfully."));
    }
}