package com.seniorapp.service;

import com.seniorapp.dto.AuthResponse;
import com.seniorapp.dto.AuthResponse.UserInfo;
import com.seniorapp.entity.OAuthState;
import com.seniorapp.entity.Role;
import com.seniorapp.entity.User;
import com.seniorapp.entity.ValidStudentId;
import com.seniorapp.repository.OAuthStateRepository;
import com.seniorapp.entity.PasswordResetToken;
import com.seniorapp.repository.PasswordResetTokenRepository;
import com.seniorapp.repository.UserRepository;
import com.seniorapp.repository.StudentWhitelistRepository;
import com.seniorapp.repository.ValidStudentIdRepository;
import com.seniorapp.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.core.ParameterizedTypeReference;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StudentWhitelistRepository whitelistRepository;
    private final OAuthStateRepository oAuthStateRepository;
    private final ValidStudentIdRepository validStudentIdRepository;

    @Value("${github.client.id}")
    private String githubClientId;

    @Value("${github.redirect.uri}")
    private String githubRedirectUri;

    @Value("${github.client.secret}")
    private String githubClientSecret;

    public AuthService(UserRepository userRepository,
                       PasswordResetTokenRepository resetTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       StudentWhitelistRepository whitelistRepository,
                       OAuthStateRepository oAuthStateRepository,
                       ValidStudentIdRepository validStudentIdRepository) {
        this.userRepository = userRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.whitelistRepository = whitelistRepository;
        this.oAuthStateRepository = oAuthStateRepository;
        this.validStudentIdRepository = validStudentIdRepository;
    }

    public AuthResponse staffLogin(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (user.getRole() == Role.STUDENT) {
            throw new RuntimeException("Students must log in via GitHub");
        }

        if (!user.isEnabled()) {
            throw new RuntimeException("Account is disabled");
        }

        if (user.getPassword() == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        return new AuthResponse(token, toUserInfo(user));
    }

    public User registerStaff(String email, String fullName, Role role) {
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already registered");
        }

        User user = new User();
        user.setEmail(email);
        user.setFullName(fullName);
        user.setRole(role);
        user.setEnabled(true);
        return userRepository.save(user);
    }

    public String generatePasswordResetToken(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setToken(UUID.randomUUID().toString());
        resetToken.setUsed(false);
        resetToken.setValidUntil(LocalDateTime.now().plusHours(24));
        resetTokenRepository.save(resetToken);

        return resetToken.getToken();
    }

    public boolean isResetTokenValid(String token) {
        return resetTokenRepository.findByTokenAndUsedFalse(token)
                .map(rt -> rt.getValidUntil().isAfter(LocalDateTime.now()))
                .orElse(false);
    }

    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = resetTokenRepository.findByTokenAndUsedFalse(token)
                .orElseThrow(() -> new RuntimeException("Invalid or already used reset token"));

        if (resetToken.getValidUntil().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Reset token has expired");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        resetTokenRepository.save(resetToken);
    }

    public AuthResponse getCurrentUser(User user) {
        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        return new AuthResponse(token, toUserInfo(user));
    }

    public List<UserInfo> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::toUserInfo)
                .toList();
    }

    public UserInfo changeUserRole(Long userId, Role newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setRole(newRole);
        userRepository.save(user);
        return toUserInfo(user);
    }

    public boolean isStudentWhitelisted(String studentId) { // Berat'ın istediği kontrol metodu
        return whitelistRepository.existsByStudentId(studentId);
    }

    private UserInfo toUserInfo(User user) {
        return new UserInfo(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name(),
                user.getGithubUsername()
        );
    }


    /**
     * GitHub OAuth callback: legacy email-based flow (no student context) or
     * student LINK / LOGIN flow using {@link OAuthState} context.
     */
    @Transactional
    public AuthResponse githubLogin(String code, String state) {
        OAuthState savedState = oAuthStateRepository.findById(state)
                .orElseThrow(() -> new RuntimeException("Invalid or expired OAuth state."));
        oAuthStateRepository.delete(savedState);

        String accessToken = getGithubAccessToken(code);
        String primaryEmail = getGithubPrimaryEmail(accessToken);
        GithubProfile gh = fetchGithubProfile(accessToken);

        if (savedState.getContextStudentId() == null || savedState.getOauthFlow() == null) {
            User user = validateAndGetUserByEmail(primaryEmail);
            String jwtToken = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());
            return new AuthResponse(jwtToken, toUserInfo(user));
        }

        return completeStudentGithubFlow(savedState, primaryEmail, gh);
    }

    private AuthResponse completeStudentGithubFlow(OAuthState savedState, String primaryEmail, GithubProfile gh) {
        String studentId = savedState.getContextStudentId();
        String flow = savedState.getOauthFlow().trim().toUpperCase();

        ValidStudentId entry = validStudentIdRepository.findByStudentId(studentId)
                .orElseThrow(() -> new RuntimeException("Student ID is no longer valid."));

        if ("LINK".equals(flow)) {
            if (entry.getAccount() != null) {
                throw new RuntimeException("This student ID already has a linked account. Sign in with GitHub instead.");
            }
            if (userRepository.findByGithubId(gh.githubId()).isPresent()) {
                throw new RuntimeException("This GitHub account is already linked to another user.");
            }
            if (userRepository.findByEmail(primaryEmail).isPresent()) {
                throw new RuntimeException("This email is already registered. Use a different GitHub account or contact support.");
            }

            User newUser = new User();
            newUser.setEmail(primaryEmail);
            newUser.setFullName(gh.displayName() != null ? gh.displayName() : gh.login());
            newUser.setRole(Role.STUDENT);
            newUser.setGithubId(gh.githubId());
            newUser.setGithubUsername(gh.login());
            newUser.setEnabled(true);
            newUser.setPassword(null);

            User saved = userRepository.save(newUser);
            entry.setAccount(saved);
            validStudentIdRepository.save(entry);

            String jwt = jwtUtil.generateToken(saved.getId(), saved.getEmail(), saved.getRole().name());
            return new AuthResponse(jwt, toUserInfo(saved));
        }

        if ("LOGIN".equals(flow)) {
            User linked = entry.getAccount();
            if (linked == null) {
                throw new RuntimeException("This student ID is not linked yet. Match your GitHub account first.");
            }
            if (linked.getGithubId() == null) {
                linked.setGithubId(gh.githubId());
                linked.setGithubUsername(gh.login());
                userRepository.save(linked);
            } else if (!linked.getGithubId().equals(gh.githubId())) {
                throw new RuntimeException("Wrong GitHub account for this student ID. Use the account you linked when registering.");
            }
            if (!linked.isEnabled()) {
                throw new RuntimeException("Student account is currently disabled.");
            }
            String jwt = jwtUtil.generateToken(linked.getId(), linked.getEmail(), linked.getRole().name());
            return new AuthResponse(jwt, toUserInfo(linked));
        }

        throw new RuntimeException("Invalid OAuth flow.");
    }

    private GithubProfile fetchGithubProfile(String accessToken) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "https://api.github.com/user",
                HttpMethod.GET,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        Map<String, Object> body = response.getBody();
        if (body == null || body.get("id") == null) {
            throw new RuntimeException("Could not read GitHub user profile.");
        }
        long id = ((Number) body.get("id")).longValue();
        String login = (String) body.get("login");
        if (login == null) {
            throw new RuntimeException("Could not read GitHub username.");
        }
        String name = body.get("name") instanceof String s ? s : null;
        return new GithubProfile(id, login, name);
    }

    private record GithubProfile(long githubId, String login, String displayName) {}

    /**
     * GitHub'dan erişim belirteci (Access Token) alır.
     */
    private String getGithubAccessToken(String code) {
        RestTemplate restTemplate = new RestTemplate();
        String tokenUrl = "https://github.com/login/oauth/access_token";

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        Map<String, String> body = Map.of(
                "client_id", githubClientId,
                "client_secret", githubClientSecret,
                "code", code,
                "redirect_uri", githubRedirectUri
        );

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

        String accessToken = (String) response.getBody().get("access_token");
        if (accessToken == null) {
            throw new RuntimeException("Failed to retrieve access token from GitHub.");
        }
        return accessToken;
    }

    /**
     * Access Token kullanarak kullanıcının birincil (primary) e-posta adresini çeker.
     */
    private String getGithubPrimaryEmail(String accessToken) {
        RestTemplate restTemplate = new RestTemplate();
        String emailUrl = "https://api.github.com/user/emails";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                emailUrl,
                HttpMethod.GET,
                request,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}
        );

        if (response.getBody() != null) {
            for (Map<String, Object> emailObj : response.getBody()) {
                if (Boolean.TRUE.equals(emailObj.get("primary"))) {
                    return (String) emailObj.get("email");
                }
            }
        }
        throw new RuntimeException("No valid primary email found in GitHub profile.");
    }

    /**
     * E-posta adresinin sistemde kayıtlı ve aktif bir öğrenciye ait olup olmadığını doğrular.
     */
    private User validateAndGetUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No authorized student account found with this email. Please contact your coordinator."));

        if (!user.isEnabled()) {
            throw new RuntimeException("Student account is currently disabled.");
        }
        return user;
    }

    /**
     * GitHub OAuth authorize URL. Optional {@code studentId} + {@code flow} ({@code LINK} / {@code LOGIN})
     * enable the student whitelist flow; omit both for legacy staff-style GitHub (email must exist in DB).
     */
    public String generateGithubAuthUrl(String studentId, String flow) {
        String state = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        if (studentId == null || studentId.isBlank() || flow == null || flow.isBlank()) {
            oAuthStateRepository.save(new OAuthState(state, now));
        } else {
            String f = flow.trim().toUpperCase();
            if (!"LINK".equals(f) && !"LOGIN".equals(f)) {
                throw new RuntimeException("flow must be LINK or LOGIN");
            }
            String sid = studentId.trim();
            ValidStudentId entry = validStudentIdRepository.findByStudentId(sid)
                    .orElseThrow(() -> new RuntimeException("Invalid student ID."));
            if ("LINK".equals(f) && entry.getAccount() != null) {
                throw new RuntimeException("This student ID already has a linked account. Sign in with GitHub instead.");
            }
            if ("LOGIN".equals(f) && entry.getAccount() == null) {
                throw new RuntimeException("This student ID is not linked yet. Match your GitHub account first.");
            }
            oAuthStateRepository.save(new OAuthState(state, now, sid, f));
        }

        String encodedRedirect = URLEncoder.encode(githubRedirectUri, StandardCharsets.UTF_8);
        return String.format(
                "https://github.com/login/oauth/authorize?client_id=%s&redirect_uri=%s&scope=read:user,user:email&state=%s",
                githubClientId, encodedRedirect, state
        );
    }
}