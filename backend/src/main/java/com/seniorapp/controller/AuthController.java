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

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.staffLogin(request.getEmail(), request.getPassword());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getCurrentUser(@AuthenticationPrincipal User user) {
        AuthResponse response = authService.getCurrentUser(user);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register-staff")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> registerStaff(@Valid @RequestBody RegisterStaffRequest request) {
        Role role;
        try {
            role = Role.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid role: " + request.getRole()));
        }

        if (role == Role.STUDENT) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cannot register students via this endpoint"));
        }

        User user = authService.registerStaff(request.getEmail(), request.getFullName(), role);
        String resetToken = authService.generatePasswordResetToken(user.getId());

        return ResponseEntity.ok(Map.of(
                "userId", user.getId(),
                "email", user.getEmail(),
                "resetLink", "/reset-password?token=" + resetToken
        ));
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserInfo>> getAllUsers() {
        return ResponseEntity.ok(authService.getAllUsers());
    }


    @GetMapping("/github/login")
    public ResponseEntity<Map<String, String>> getGithubLoginUrl() {
        String authUrl = authService.generateGithubAuthUrl();
        return ResponseEntity.ok(Map.of("authUrl", authUrl));
    }
    /**
     * GitHub yetkilendirmesi sonrasında yönlendirilen callback endpoint'i.
     * Dönen JWT token frontend tarafından yakalanıp saklanacaktır.
     */
    @GetMapping("/github/callback")
    public ResponseEntity<AuthResponse> githubCallback(@RequestParam String code,
                                                       @RequestParam(required = false) String state) {


        AuthResponse response = authService.githubLogin(code);
        return ResponseEntity.ok(response);
    }


    @PutMapping("/users/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> changeUserRole(@Valid @RequestBody ChangeRoleRequest request) {
        Role role;
        try {
            role = Role.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid role: " + request.getRole()));
        }
        UserInfo updated = authService.changeUserRole(request.getUserId(), role);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password has been reset successfully"));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleError(RuntimeException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
