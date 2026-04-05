package com.seniorapp.controller;

import com.seniorapp.service.LogService;
import com.seniorapp.service.PasswordResetService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth/reset-password")
public class AuthResetController {

    private static final Logger log = LoggerFactory.getLogger(AuthResetController.class);

    private final PasswordResetService passwordResetService;
    private final LogService logService;

    public AuthResetController(PasswordResetService passwordResetService, LogService logService) {
        this.passwordResetService = passwordResetService;
        this.logService = logService;
    }

    // Triggered by Admin from the dashboard
    @PostMapping("/admin")
    public ResponseEntity<?> resetByAdmin(@RequestBody Map<String, Long> payload,
                                            HttpServletRequest httpRequest) {
        Long accountId = payload.get("accountId");
        passwordResetService.generateAndSendTokenByUserId(accountId);
        log.info("Password reset email triggered by admin for userId={}", accountId);
        try {
            logService.saveLog(null, null, "authentication", "password_reset_email_admin",
                    "success", "info", "Admin requested reset for userId=" + accountId, httpRequest);
        } catch (Exception e) {
            log.warn("Could not persist password reset audit", e);
        }
        return ResponseEntity.ok(Map.of("message", "Reset link sent successfully."));
    }

    // Forgot password triggered via Email
    @PostMapping("/forgot")
    public ResponseEntity<?> resetByEmail(@RequestBody Map<String, String> payload,
                                          HttpServletRequest httpRequest) {
        String email = payload.get("email");
        passwordResetService.generateAndSendTokenByEmail(email);
        log.info("Forgot-password flow invoked (email redacted)");
        try {
            logService.saveLog(null, null, "authentication", "password_reset_email_forgot",
                    "success", "info", "Forgot-password flow", httpRequest);
        } catch (Exception e) {
            log.warn("Could not persist forgot-password audit", e);
        }
        return ResponseEntity.ok(Map.of("message", "If the email exists, a reset link has been sent."));
    }

    // Check token validity
    @GetMapping("/check-token-validity")
    public ResponseEntity<?> checkTokenValidity(@RequestParam String token) {
        boolean isValid = passwordResetService.isTokenValid(token);
        log.debug("Token validity check: valid={}", isValid);
        return ResponseEntity.ok(Map.of("valid", isValid));
    }

    // Save the new password
    @PostMapping("/new-password")
    public ResponseEntity<?> setNewPassword(@RequestBody Map<String, String> payload,
                                            HttpServletRequest httpRequest) {
        String token = payload.get("token");
        String newPassword = payload.get("newPassword");
        passwordResetService.updatePassword(token, newPassword);
        log.info("Password updated via reset token flow");
        try {
            logService.saveLog(null, null, "authentication", "password_reset_new_password",
                    "success", "info", "Password set via reset token", httpRequest);
        } catch (Exception e) {
            log.warn("Could not persist new-password audit", e);
        }
        return ResponseEntity.ok(Map.of("message", "Password updated successfully."));
    }
}