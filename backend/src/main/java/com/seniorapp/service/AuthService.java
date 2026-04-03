package com.seniorapp.service;

import com.seniorapp.dto.AuthResponse;
import com.seniorapp.dto.AuthResponse.UserInfo;
import com.seniorapp.entity.PasswordResetToken;
import com.seniorapp.entity.Role;
import com.seniorapp.entity.User;
import com.seniorapp.repository.PasswordResetTokenRepository;
import com.seniorapp.repository.UserRepository;
import com.seniorapp.repository.StudentWhitelistRepository; // Import eklendi
import com.seniorapp.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StudentWhitelistRepository whitelistRepository; // Değişken eklendi

    public AuthService(UserRepository userRepository,
                       PasswordResetTokenRepository resetTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       StudentWhitelistRepository whitelistRepository) { // Parametre eklendi
        this.userRepository = userRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.whitelistRepository = whitelistRepository; // Atama eklendi
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
}