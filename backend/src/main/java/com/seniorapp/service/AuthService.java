package com.seniorapp.service;

import com.seniorapp.dto.AuthResponse;
import com.seniorapp.dto.AuthResponse.UserInfo;
import com.seniorapp.entity.OAuthState;
import com.seniorapp.entity.Role;
import com.seniorapp.entity.User;
import com.seniorapp.repository.OAuthStateRepository;
import com.seniorapp.repository.UserRepository;
import com.seniorapp.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.core.ParameterizedTypeReference;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final OAuthStateRepository oAuthStateRepository;

    @Value("${github.client.id}")
    private String githubClientId;

    @Value("${github.redirect.uri}")
    private String githubRedirectUri;

    @Value("${github.client.secret}")
    private String githubClientSecret;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil, OAuthStateRepository oAuthStateRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.oAuthStateRepository = oAuthStateRepository;
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

        String token = UUID.randomUUID().toString();
        user.setPasswordResetToken(token);
        user.setPasswordResetExpiry(LocalDateTime.now().plusHours(24));
        userRepository.save(user);

        return token;
    }

    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid reset token"));

        if (user.getPasswordResetExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Reset token has expired");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiry(null);
        userRepository.save(user);
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

    private UserInfo toUserInfo(User user) {
        return new UserInfo(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name(),
                user.getGithubUsername(),
                user.getStudentId()
        );
    }

    /**
     * GitHub OAuth2 akışını yöneten ana metod.
     * Gelen state bilgisini güvenlik (CSRF) için veritabanından doğrular.
     */
    public AuthResponse githubLogin(String code, String state) {
        OAuthState savedState = oAuthStateRepository.findById(state)
                .orElseThrow(() -> new RuntimeException("Invalid or missing CSRF state token. Security breach detected."));


        oAuthStateRepository.delete(savedState);

        String accessToken = getGithubAccessToken(code);
        String primaryEmail = getGithubPrimaryEmail(accessToken);
        User user = validateAndGetUserByEmail(primaryEmail);

        String jwtToken = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        return new AuthResponse(jwtToken, toUserInfo(user));
    }

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
     * GitHub OAuth2 Authorization URL'ini oluşturur ve state'i DB'ye kaydeder.
     */
    public String generateGithubAuthUrl() {
        String state = UUID.randomUUID().toString();


        oAuthStateRepository.save(new OAuthState(state, LocalDateTime.now()));

        return String.format(
                "https://github.com/login/oauth/authorize?client_id=%s&redirect_uri=%s&scope=read:user,user:email&state=%s",
                githubClientId, githubRedirectUri, state
        );
    }
}