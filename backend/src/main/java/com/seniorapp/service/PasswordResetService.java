package com.seniorapp.service;

import com.seniorapp.entity.PasswordResetToken;
import com.seniorapp.entity.User;
import com.seniorapp.repository.PasswordResetTokenRepository;
import com.seniorapp.repository.UserRepository;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder; 
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder; 

    // Constructor updated to include PasswordEncoder
    public PasswordResetService(UserRepository userRepository, 
                                PasswordResetTokenRepository tokenRepository, 
                                JavaMailSender mailSender,
                                PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.mailSender = mailSender;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void generateAndSendTokenByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
                
        // Ensure the user is a staff member. Students use GitHub OAuth.
        if ("STUDENT".equals(user.getRole().name())) {
            throw new RuntimeException("Password reset is only available for staff members. Students must log in via GitHub.");
        }

        createTokenAndSendEmail(user);
    }

    @Transactional
    public void generateAndSendTokenByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with this email"));
                
        // Ensure the user is a staff member. Students use GitHub OAuth.
        if ("STUDENT".equals(user.getRole().name())) {
            throw new RuntimeException("Password reset is only available for staff members. Students must log in via GitHub.");
        }

        createTokenAndSendEmail(user);
    }

    private void createTokenAndSendEmail(User user) {
        // Invalidate old unused tokens
        List<PasswordResetToken> oldTokens = tokenRepository.findByUserAndUsedFalse(user);
        for (PasswordResetToken oldToken : oldTokens) {
            oldToken.setUsed(true);
        }
        tokenRepository.saveAll(oldTokens);

        // Create a new token
        PasswordResetToken newToken = new PasswordResetToken();
        newToken.setUser(user);
        newToken.setToken(UUID.randomUUID().toString());
        newToken.setValidUntil(LocalDateTime.now().plusHours(24)); // Valid for 24 hours
        newToken.setUsed(false);
        tokenRepository.save(newToken);

        String resetLink = "http://localhost:5173/reset-password?token=" + newToken.getToken();
        
        // Actual email sending process
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject("Password Reset Request");
        message.setText("To reset your password, please click the link below:\n\n" + resetLink);
        mailSender.send(message);
    }

    public boolean isTokenValid(String tokenStr) {
        PasswordResetToken token = tokenRepository.findByToken(tokenStr).orElse(null);
        if (token == null || token.isUsed()) {
            return false;
        }
        return token.getValidUntil().isAfter(LocalDateTime.now());
    }

    @Transactional
    public void updatePassword(String tokenStr, String newPassword) {
        PasswordResetToken token = tokenRepository.findByToken(tokenStr)
                .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (token.isUsed() || token.getValidUntil().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token is expired or already used");
        }

        User user = token.getUser();
        
        // Encode the new password before saving it to the database
        user.setPassword(passwordEncoder.encode(newPassword)); 
        userRepository.save(user);

        // Mark token as used
        token.setUsed(true);
        tokenRepository.save(token);
    }
}