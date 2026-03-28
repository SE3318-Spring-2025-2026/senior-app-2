package com.seniorapp.service;

import com.seniorapp.dto.AuthResponse;
import com.seniorapp.dto.AuthResponse.UserInfo;
import com.seniorapp.entity.Role;
import com.seniorapp.entity.User;
import com.seniorapp.repository.UserRepository;
import com.seniorapp.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
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
}
