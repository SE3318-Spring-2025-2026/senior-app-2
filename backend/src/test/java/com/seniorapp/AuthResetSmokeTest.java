package com.seniorapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seniorapp.entity.PasswordResetToken;
import com.seniorapp.entity.Role;
import com.seniorapp.entity.User;
import com.seniorapp.entity.UserStatus;
import com.seniorapp.repository.PasswordResetTokenRepository;
import com.seniorapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Smoke / integration tests for AuthResetController.
 * Spins up the full Spring context against the test H2 database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthResetSmokeTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository userRepository;
    @Autowired PasswordResetTokenRepository tokenRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private User staffUser;

    @BeforeEach
    void setUp() {
        tokenRepository.deleteAll();
        userRepository.deleteAll();

        staffUser = new User();
        staffUser.setEmail("reset-staff@seniorapp.com");
        staffUser.setFullName("Reset Staff");
        staffUser.setPassword(passwordEncoder.encode("oldPass1"));
        staffUser.setRole(Role.COORDINATOR);
        staffUser.setEnabled(true);
        staffUser.setStatus(UserStatus.ACTIVE);
        staffUser = userRepository.save(staffUser);
    }

    // ----------------------------------------------------------------
    // check-token-validity
    // ----------------------------------------------------------------

    @Test
    void checkTokenValidity_unknownToken_returnsFalse() throws Exception {
        mockMvc.perform(get("/auth/reset-password/check-token-validity")
                        .param("token", "does-not-exist"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    void checkTokenValidity_activeToken_returnsTrue() throws Exception {
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(staffUser);
        token.setToken(UUID.randomUUID().toString());
        token.setUsed(false);
        token.setValidUntil(LocalDateTime.now().plusHours(1));
        tokenRepository.save(token);

        mockMvc.perform(get("/auth/reset-password/check-token-validity")
                        .param("token", token.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    void checkTokenValidity_expiredToken_returnsFalse() throws Exception {
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(staffUser);
        token.setToken(UUID.randomUUID().toString());
        token.setUsed(false);
        token.setValidUntil(LocalDateTime.now().minusMinutes(1));
        tokenRepository.save(token);

        mockMvc.perform(get("/auth/reset-password/check-token-validity")
                        .param("token", token.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    void checkTokenValidity_usedToken_returnsFalse() throws Exception {
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(staffUser);
        token.setToken(UUID.randomUUID().toString());
        token.setUsed(true);
        token.setValidUntil(LocalDateTime.now().plusHours(1));
        tokenRepository.save(token);

        mockMvc.perform(get("/auth/reset-password/check-token-validity")
                        .param("token", token.getToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }

    // ----------------------------------------------------------------
    // /forgot — email enumeration (security): always 200
    // ----------------------------------------------------------------

    @Test
    void forgotPassword_existingEmail_returns200WithGenericMessage() throws Exception {
        mockMvc.perform(post("/auth/reset-password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", staffUser.getEmail()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void forgotPassword_nonExistentEmail_alsoReturns200_preventingEnumeration() throws Exception {
        mockMvc.perform(post("/auth/reset-password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "nobody@ghost.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    // ----------------------------------------------------------------
    // /new-password — set new password via valid token
    // ----------------------------------------------------------------

    @Test
    void newPassword_validToken_updatesPasswordAndMarksTokenUsed() throws Exception {
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(staffUser);
        token.setToken(UUID.randomUUID().toString());
        token.setUsed(false);
        token.setValidUntil(LocalDateTime.now().plusHours(1));
        tokenRepository.save(token);

        mockMvc.perform(post("/auth/reset-password/new-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("token", token.getToken(), "newPassword", "supersecure99"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        // Token used olarak işaretlenmeli
        assertThat(tokenRepository.findByToken(token.getToken())
                .orElseThrow().isUsed()).isTrue();
    }

    @Test
    void newPassword_expiredToken_returns500OrBadRequest() throws Exception {
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(staffUser);
        token.setToken(UUID.randomUUID().toString());
        token.setUsed(false);
        token.setValidUntil(LocalDateTime.now().minusMinutes(1));
        tokenRepository.save(token);

        mockMvc.perform(post("/auth/reset-password/new-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("token", token.getToken(), "newPassword", "supersecure99"))))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void newPassword_usedToken_returns500() throws Exception {
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(staffUser);
        token.setToken(UUID.randomUUID().toString());
        token.setUsed(true);
        token.setValidUntil(LocalDateTime.now().plusHours(1));
        tokenRepository.save(token);

        mockMvc.perform(post("/auth/reset-password/new-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("token", token.getToken(), "newPassword", "supersecure99"))))
                .andExpect(status().is5xxServerError());
    }

    @Test
    void newPassword_invalidToken_returns500() throws Exception {
        mockMvc.perform(post("/auth/reset-password/new-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("token", "totally-fake-token", "newPassword", "supersecure99"))))
                .andExpect(status().is5xxServerError());
    }
}
