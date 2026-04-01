package com.seniorapp.controller;

import com.seniorapp.dto.AuthResponse;
import com.seniorapp.dto.AuthResponse.UserInfo;
import com.seniorapp.dto.ChangeRoleRequest;
import com.seniorapp.dto.LoginRequest;
import com.seniorapp.dto.PasswordResetRequest;
import com.seniorapp.dto.RegisterStaffRequest;
import com.seniorapp.entity.Role;
import com.seniorapp.entity.User;
import com.seniorapp.service.AuthService;
import com.seniorapp.service.LogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final LogService  logService;

    public AuthController(AuthService authService, LogService logService) {
        this.authService = authService;
        this.logService  = logService;
    }

    // -------------------------------------------------------
    // POST /api/auth/login
    // -------------------------------------------------------
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest) {
        try {
            AuthResponse response = authService.staffLogin(request.getEmail(), request.getPassword());

            // ✅ Başarılı giriş logu
            logService.saveAuthLog(
                    response.getUser().getId(),
                    response.getUser().getRole(),
                    "staff_logged_in",
                    "success",
                    "Staff user logged in: " + request.getEmail(),
                    httpRequest
            );

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            // ❌ Başarısız giriş logu
            logService.saveAuthLog(
                    null,
                    null,
                    "login_failed",
                    "failed",
                    "Failed login attempt for email: " + request.getEmail() + " | Reason: " + e.getMessage(),
                    httpRequest
            );
            throw e;
        }
    }

    // -------------------------------------------------------
    // GET /api/auth/me
    // -------------------------------------------------------
    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getCurrentUser(@AuthenticationPrincipal User user) {
        AuthResponse response = authService.getCurrentUser(user);
        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------
    // POST /api/auth/register-staff  (ADMIN only)
    // -------------------------------------------------------
    @PostMapping("/register-staff")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> registerStaff(
            @Valid @RequestBody RegisterStaffRequest request,
            @AuthenticationPrincipal User adminUser,
            HttpServletRequest httpRequest) {

        Role role;
        try {
            role = Role.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid role: " + request.getRole()));
        }

        if (role == Role.STUDENT) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cannot register students via this endpoint"));
        }

        User newUser    = authService.registerStaff(request.getEmail(), request.getFullName(), role);
        String resetToken = authService.generatePasswordResetToken(newUser.getId());

        // 📝 Yeni staff kaydı logu
        logService.saveLog(
                adminUser != null ? adminUser.getId() : null,
                adminUser != null ? adminUser.getRole().name() : "ADMIN",
                "user_management",
                "staff_registered",
                "success",
                "info",
                "New staff registered: " + newUser.getEmail() + " | Role: " + role.name(),
                httpRequest
        );

        return ResponseEntity.ok(Map.of(
                "userId",    newUser.getId(),
                "email",     newUser.getEmail(),
                "resetLink", "/reset-password?token=" + resetToken
        ));
    }

    // -------------------------------------------------------
    // GET /api/auth/users  (ADMIN only)
    // -------------------------------------------------------
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserInfo>> getAllUsers() {
        return ResponseEntity.ok(authService.getAllUsers());
    }

    // -------------------------------------------------------
    // PUT /api/auth/users/role  (ADMIN only)
    // -------------------------------------------------------
    @PutMapping("/users/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> changeUserRole(@Valid @RequestBody ChangeRoleRequest request,
                                            @AuthenticationPrincipal User adminUser,
                                            HttpServletRequest httpRequest) {
        Role role;
        try {
            role = Role.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid role: " + request.getRole()));
        }

        UserInfo updated = authService.changeUserRole(request.getUserId(), role);

        // 📝 Rol değişikliği logu
        logService.saveLog(
                adminUser != null ? adminUser.getId() : null,
                adminUser != null ? adminUser.getRole().name() : "ADMIN",
                "user_management",
                "role_changed",
                "success",
                "info",
                "Role changed for userId=" + request.getUserId() + " → " + role.name(),
                httpRequest
        );

        return ResponseEntity.ok(updated);
    }

    // -------------------------------------------------------
    // POST /api/auth/reset-password
    // -------------------------------------------------------
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody PasswordResetRequest request,
            HttpServletRequest httpRequest) {

        authService.resetPassword(request.getToken(), request.getNewPassword());

        // 📝 Şifre sıfırlama logu
        logService.saveLog(
                null,
                null,
                "authentication",
                "password_reset",
                "success",
                "info",
                "Password reset completed via token",
                httpRequest
        );

        return ResponseEntity.ok(Map.of("message", "Password has been reset successfully"));
    }

    // -------------------------------------------------------
    // Exception handler
    // -------------------------------------------------------
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleError(RuntimeException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
