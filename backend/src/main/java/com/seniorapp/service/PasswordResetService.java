package com.seniorapp.service;

import com.seniorapp.entity.PasswordResetToken;
import com.seniorapp.entity.User;
import com.seniorapp.repository.PasswordResetTokenRepository;
import com.seniorapp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetTokenRepository tokenRepository,
                                EmailService emailService,
                                PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void generateAndSendTokenByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Students authenticate via GitHub OAuth, not password reset.
        if ("STUDENT".equals(user.getRole().name())) {
            throw new RuntimeException("Password reset is only available for staff members. Students must log in via GitHub.");
        }

        createTokenAndSendEmail(user);
    }

    /**
     * Forgot-password flow triggered via email form.
     * Silently does nothing if the email is not found — prevents email enumeration attacks.
     */
    @Transactional
    public void generateAndSendTokenByEmail(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            // Students authenticate via GitHub OAuth, not password reset.
            if ("STUDENT".equals(user.getRole().name())) {
                log.warn("Forgot-password request ignored for STUDENT account (email suppressed)");
                return;
            }
            createTokenAndSendEmail(user);
        });
    }

    private void createTokenAndSendEmail(User user) {
        // Hard-delete all previous tokens for this user so old links are immediately invalid.
        tokenRepository.deleteByUser(user);
        tokenRepository.flush();

        // Issue a fresh token valid for 24 hours.
        PasswordResetToken newToken = new PasswordResetToken();
        newToken.setUser(user);
        newToken.setToken(UUID.randomUUID().toString());
        newToken.setValidUntil(LocalDateTime.now().plusHours(24));
        newToken.setUsed(false);
        tokenRepository.save(newToken);

        String resetLink = "http://localhost:5173/reset-password?token=" + newToken.getToken();
        String body = "To reset your password, please click the link below:\n\n" + resetLink
                + "\n\nThis link will expire in 24 hours. If you did not request a password reset, you can ignore this email.";
        try {
            emailService.sendPlainText(user.getEmail(), "Password Reset Request", body);
            log.info("Password reset email sent userId={}", user.getId());
        } catch (RuntimeException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new RuntimeException("Failed to send password reset email.", cause);
        }
    }

    /**
     * Returns {@code true} only when the token exists, is not used, and has not expired.
     * Uses the narrower {@code findByTokenAndUsedFalse} query to avoid loading expired/used rows unnecessarily.
     */
    public boolean isTokenValid(String tokenStr) {
        return tokenRepository.findByTokenAndUsedFalse(tokenStr)
                .map(t -> t.getValidUntil().isAfter(LocalDateTime.now()))
                .orElse(false);
    }

    @Transactional
    public void updatePassword(String tokenStr, String newPassword) {
        if (tokenStr == null || tokenStr.isBlank()) {
            throw new RuntimeException("Token must not be blank");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new RuntimeException("Password must be at least 6 characters long");
        }

        PasswordResetToken token = tokenRepository.findByToken(tokenStr)
                .orElseThrow(() -> new RuntimeException("Invalid or unknown token"));

        if (token.isUsed()) {
            throw new RuntimeException("This reset link has already been used");
        }
        if (token.getValidUntil().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("This reset link has expired");
        }

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Mark token as used so it cannot be replayed.
        token.setUsed(true);
        tokenRepository.save(token);
        log.info("Password updated via reset token for userId={}", user.getId());
    }
}