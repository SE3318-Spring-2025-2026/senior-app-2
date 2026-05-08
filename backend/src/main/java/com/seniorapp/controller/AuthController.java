package com.seniorapp.controller;

import com.seniorapp.dto.AuthResponse;
import com.seniorapp.dto.AuthResponse.UserInfo;
import com.seniorapp.dto.ChangeRoleRequest;
import com.seniorapp.dto.LoginRequest;
import com.seniorapp.dto.PasswordResetRequest;
import com.seniorapp.dto.ProfileGithubPatRequest;
import com.seniorapp.dto.ProfileGithubPatStatusResponse;
import com.seniorapp.dto.ProfileJiraAccountRequest;
import com.seniorapp.dto.ProfileJiraAccountResponse;
import com.seniorapp.dto.ProfileJiraConnectionRequest;
import com.seniorapp.dto.ProfileJiraConnectionStatusResponse;
import com.seniorapp.dto.ProfileUpdateRequest;
import com.seniorapp.dto.RegisterStaffRequest;
import com.seniorapp.dto.UserResponseDTO;
import com.seniorapp.entity.Role;
import com.seniorapp.entity.User;
import com.seniorapp.repository.UserRepository;
import com.seniorapp.service.IntegrationCredentialCryptoService;
import com.seniorapp.service.AuthService;
import com.seniorapp.service.JiraOAuthService;
import com.seniorapp.service.LogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.net.URI;
import java.net.URISyntaxException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final LogService  logService;
    private final UserRepository userRepository;
    private final IntegrationCredentialCryptoService cryptoService;
    private final JiraOAuthService jiraOAuthService;

    public AuthController(
            AuthService authService,
            LogService logService,
            UserRepository userRepository,
            IntegrationCredentialCryptoService cryptoService,
            JiraOAuthService jiraOAuthService) {
        this.authService = authService;
        this.logService  = logService;
        this.userRepository = userRepository;
        this.cryptoService = cryptoService;
        this.jiraOAuthService = jiraOAuthService;
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
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return ResponseEntity.ok(UserResponseDTO.from(user));
    }

    @PutMapping("/profile")
    public ResponseEntity<AuthResponse.UserInfo> updateProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ProfileUpdateRequest request) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        Long userId = Objects.requireNonNull(user.getId(), "Authenticated user id is required.");
        return ResponseEntity.ok(authService.updateProfile(userId, request.getFullName(), request.getEmail()));
    }

    @GetMapping("/profile/github-pat")
    public ResponseEntity<ProfileGithubPatStatusResponse> getProfileGithubPat(@AuthenticationPrincipal User user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        Long userId = Objects.requireNonNull(user.getId(), "Authenticated user id is required.");
        User fresh = userRepository.findById(userId).orElseThrow();
        boolean configured = fresh.getGithubPatEncrypted() != null && !fresh.getGithubPatEncrypted().isBlank();
        return ResponseEntity.ok(new ProfileGithubPatStatusResponse(configured));
    }

    @PutMapping("/profile/github-pat")
    public ResponseEntity<Map<String, Object>> saveProfileGithubPat(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ProfileGithubPatRequest request) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        Long userId = Objects.requireNonNull(user.getId(), "Authenticated user id is required.");
        User fresh = userRepository.findById(userId).orElseThrow();
        fresh.setGithubPatEncrypted(cryptoService.encrypt(request.getGithubPat().trim()));
        userRepository.save(fresh);
        return ResponseEntity.ok(Map.of("status", "success", "message", "GitHub PAT saved."));
    }

    @GetMapping("/profile/jira-connection")
    public ResponseEntity<ProfileJiraConnectionStatusResponse> getProfileJiraConnection(
            @AuthenticationPrincipal User user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        Long userId = Objects.requireNonNull(user.getId(), "Authenticated user id is required.");
        User fresh = userRepository.findById(userId).orElseThrow();
        boolean configured = fresh.getJiraApiTokenEncrypted() != null && !fresh.getJiraApiTokenEncrypted().isBlank();
        String jiraSiteUrl = null;
        if (fresh.getJiraSiteUrlEncrypted() != null && !fresh.getJiraSiteUrlEncrypted().isBlank()) {
            jiraSiteUrl = cryptoService.decrypt(fresh.getJiraSiteUrlEncrypted());
        }
        return ResponseEntity.ok(new ProfileJiraConnectionStatusResponse(configured, jiraSiteUrl));
    }

    @GetMapping("/jira/oauth/login")
    public ResponseEntity<Map<String, String>> getJiraOAuthLoginUrl(@AuthenticationPrincipal User user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        String authUrl = jiraOAuthService.generateAuthUrl(user.getId());
        return ResponseEntity.ok(Map.of("authUrl", authUrl));
    }

    @GetMapping("/jira/oauth/callback")
    public ResponseEntity<Void> jiraOauthCallback(@RequestParam String code, @RequestParam String state) {
        String redirect = jiraOAuthService.handleCallback(code, state);
        return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, redirect).build();
    }

    @PutMapping("/profile/jira-connection")
    public ResponseEntity<Map<String, Object>> saveProfileJiraConnection(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ProfileJiraConnectionRequest request) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        Long userId = Objects.requireNonNull(user.getId(), "Authenticated user id is required.");
        String jiraSiteUrl = request.getJiraSiteUrl().trim();
        String jiraApiToken = request.getJiraApiToken().trim();
        validateJiraUrl(jiraSiteUrl);

        User fresh = userRepository.findById(userId).orElseThrow();
        fresh.setJiraSiteUrlEncrypted(cryptoService.encrypt(jiraSiteUrl));
        fresh.setJiraApiTokenEncrypted(cryptoService.encrypt(jiraApiToken));
        userRepository.save(fresh);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Jira connection saved."));
    }

    @PutMapping("/profile/jira-account")
    public ResponseEntity<Map<String, Object>> saveProfileJiraAccount(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ProfileJiraAccountRequest request) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        Long userId = Objects.requireNonNull(user.getId(), "Authenticated user id is required.");
        User fresh = userRepository.findById(userId).orElseThrow();
        fresh.setJiraAccountId(request.getJiraAccountId() == null ? null : request.getJiraAccountId().trim());
        fresh.setJiraEmail(request.getJiraEmail() == null ? null : request.getJiraEmail().trim());
        fresh.setJiraDisplayName(request.getJiraDisplayName() == null ? null : request.getJiraDisplayName().trim());
        userRepository.save(fresh);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Jira account saved."));
    }

    @GetMapping("/profile/jira-account")
    public ResponseEntity<ProfileJiraAccountResponse> getProfileJiraAccount(
            @AuthenticationPrincipal User user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        Long userId = Objects.requireNonNull(user.getId(), "Authenticated user id is required.");
        User fresh = userRepository.findById(userId).orElseThrow();
        return ResponseEntity.ok(new ProfileJiraAccountResponse(
                fresh.getJiraAccountId(),
                fresh.getJiraEmail(),
                fresh.getJiraDisplayName()));
    }

    private void validateJiraUrl(String jiraSiteUrl) {
        try {
            URI uri = new URI(jiraSiteUrl);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (host == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Jira site URL format.");
            }
        } catch (URISyntaxException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Jira site URL format.");
        }
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
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        Long userId = null;
        String role = null;
        if (authentication != null && authentication.getPrincipal() instanceof User u) {
            userId = u.getId();
            role = u.getRole().name();
        }
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        try {
            logService.saveAuthLog(userId, role, "logout", "success",
                    "Client logout / session cleared", request);
        } catch (Exception ignored) {
            // avoid masking logout success if audit write fails
        }
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
                                                       @RequestParam String state,
                                                       HttpServletRequest httpRequest) {
        try {
            AuthResponse response = authService.githubLogin(code, state);
            logService.saveAuthLog(
                    response.getUser().getId(),
                    response.getUser().getRole(),
                    "github_oauth_callback",
                    "success",
                    "GitHub OAuth completed for userId=" + response.getUser().getId(),
                    httpRequest
            );
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logService.saveAuthLog(
                    null,
                    null,
                    "github_oauth_callback",
                    "failed",
                    e.getMessage() != null ? e.getMessage() : "GitHub OAuth failed",
                    httpRequest
            );
            throw e;
        }
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
