package com.seniorapp.controller;

import com.seniorapp.dto.AuthResponse;
import com.seniorapp.dto.AuthResponse.UserInfo;
import com.seniorapp.dto.ChangeRoleRequest;
import com.seniorapp.dto.LoginRequest;
import com.seniorapp.dto.PasswordResetRequest;
import com.seniorapp.dto.RegisterStaffRequest;
import com.seniorapp.dto.UserResponseDTO;
import com.seniorapp.entity.Role;
import com.seniorapp.entity.User;
import com.seniorapp.service.AuthService;
import com.seniorapp.service.LogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
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
    // GET /api/auth/me  (legacy – returns AuthResponse with a refreshed token)
    // -------------------------------------------------------
    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getCurrentUser(@AuthenticationPrincipal User user) {
        AuthResponse response = authService.getCurrentUser(user);
        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------
    // GET /api/auth/profile  (new – returns lightweight UserResponseDTO)
    // -------------------------------------------------------

    /**
     * Returns the currently authenticated user's profile as a lightweight DTO.
     * Does NOT issue a new JWT — intended for "who am I?" checks.
     *
     * @param user the principal injected by the JWT filter
     * @return 200 OK with {@link UserResponseDTO}
     */
    @GetMapping("/profile")
    public ResponseEntity<UserResponseDTO> getProfile(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(UserResponseDTO.from(user));
    }

    // -------------------------------------------------------
    // GET /api/auth/logout
    // -------------------------------------------------------

    /**
     * Invalidates the current HTTP session (if any) and clears the security
     * context.  The client is responsible for discarding its JWT.
     *
     * @return 200 OK with a confirmation message
     */
    @GetMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request,
                                                      HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("message", "Logged out successfully. Please discard your token."));
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

    @GetMapping("/github/login")
    public ResponseEntity<Map<String, String>> getGithubLoginUrl(
            @RequestParam(required = false) String studentId,
            @RequestParam(required = false) String flow) {
        String authUrl = authService.generateGithubAuthUrl(studentId, flow);
        return ResponseEntity.ok(Map.of("authUrl", authUrl));
    }

    /**
     * GitHub redirects here with {@code code} and {@code state}; SPA also calls this with fetch after redirect to Vite.
     */
    @GetMapping("/github/callback")
    public ResponseEntity<AuthResponse> githubCallback(@RequestParam String code,
                                                       @RequestParam String state) {
        return ResponseEntity.ok(authService.githubLogin(code, state));
    }


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
