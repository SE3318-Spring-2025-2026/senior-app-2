package com.seniorapp.service;

import com.seniorapp.entity.PasswordResetToken;
import com.seniorapp.entity.Role;
import com.seniorapp.entity.User;
import com.seniorapp.entity.UserStatus;
import com.seniorapp.repository.PasswordResetTokenRepository;
import com.seniorapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Sert unit testler — her davranış izole Mockito ile doğrulanır.
 * Veritabanı veya Spring context açılmaz.
 */
class PasswordResetServiceTest {

    // ---- mocks ----
    private UserRepository userRepository;
    private PasswordResetTokenRepository tokenRepository;
    private EmailService emailService;
    private PasswordEncoder passwordEncoder;
    private PasswordResetService service;

    // ---- helper builders ----
    private static User staffUser(Long id, String email) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setPassword("hashed");
        u.setRole(Role.COORDINATOR);
        u.setEnabled(true);
        u.setStatus(UserStatus.ACTIVE);
        return u;
    }

    private static User studentUser(Long id, String email) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setPassword("");
        u.setRole(Role.STUDENT);
        u.setEnabled(true);
        u.setStatus(UserStatus.ACTIVE);
        return u;
    }

    private static PasswordResetToken activeToken(User user, String tokenStr) {
        PasswordResetToken t = new PasswordResetToken();
        t.setUser(user);
        t.setToken(tokenStr);
        t.setUsed(false);
        t.setValidUntil(LocalDateTime.now().plusHours(1));
        return t;
    }

    private static PasswordResetToken expiredToken(User user, String tokenStr) {
        PasswordResetToken t = new PasswordResetToken();
        t.setUser(user);
        t.setToken(tokenStr);
        t.setUsed(false);
        t.setValidUntil(LocalDateTime.now().minusSeconds(1));
        return t;
    }

    private static PasswordResetToken usedToken(User user, String tokenStr) {
        PasswordResetToken t = new PasswordResetToken();
        t.setUser(user);
        t.setToken(tokenStr);
        t.setUsed(true);
        t.setValidUntil(LocalDateTime.now().plusHours(1));
        return t;
    }

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        tokenRepository = mock(PasswordResetTokenRepository.class);
        emailService = mock(EmailService.class);
        passwordEncoder = mock(PasswordEncoder.class);

        when(passwordEncoder.encode(anyString())).thenAnswer(inv -> "ENCODED_" + inv.getArgument(0));
        when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service = new PasswordResetService(userRepository, tokenRepository, emailService, passwordEncoder);
    }

    // ----------------------------------------------------------------
    // generateAndSendTokenByEmail — güvenlik / happy path
    // ----------------------------------------------------------------

    @Test
    void forgotByEmail_existingStaff_deletesOldTokensAndSavesNewAndSendsEmail() {
        User staff = staffUser(1L, "prof@uni.edu");
        when(userRepository.findByEmail("prof@uni.edu")).thenReturn(Optional.of(staff));

        service.generateAndSendTokenByEmail("prof@uni.edu");

        // Eski tokenlar hard-delete edilmeli
        verify(tokenRepository).deleteByUser(staff);
        verify(tokenRepository).flush();

        // Yeni token kaydedilmeli
        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(captor.capture());
        PasswordResetToken saved = captor.getValue();
        assertThat(saved.isUsed()).isFalse();
        assertThat(saved.getValidUntil()).isAfter(LocalDateTime.now());
        assertThat(saved.getToken()).isNotBlank();

        // Email gönderilmeli
        verify(emailService).sendPlainText(eq("prof@uni.edu"), anyString(), anyString());
    }

    @Test
    void forgotByEmail_nonExistentEmail_doesNothingNoException() {
        when(userRepository.findByEmail("ghost@uni.edu")).thenReturn(Optional.empty());

        // Email enumeration'a karşı: exception fırlatmamalı, sessizce dönmeli
        assertThatCode(() -> service.generateAndSendTokenByEmail("ghost@uni.edu"))
                .doesNotThrowAnyException();

        verifyNoInteractions(tokenRepository, emailService);
    }

    @Test
    void forgotByEmail_studentAccount_silentlyIgnoresWithoutSendingEmail() {
        User student = studentUser(5L, "stu@uni.edu");
        when(userRepository.findByEmail("stu@uni.edu")).thenReturn(Optional.of(student));

        // Öğrenci hesabına reset maili gönderilmemeli
        assertThatCode(() -> service.generateAndSendTokenByEmail("stu@uni.edu"))
                .doesNotThrowAnyException();

        verifyNoInteractions(tokenRepository, emailService);
    }

    // ----------------------------------------------------------------
    // generateAndSendTokenByUserId
    // ----------------------------------------------------------------

    @Test
    void forgotByUserId_studentAccount_throwsDescriptiveException() {
        User student = studentUser(10L, "s@uni.edu");
        when(userRepository.findById(10L)).thenReturn(Optional.of(student));

        assertThatThrownBy(() -> service.generateAndSendTokenByUserId(10L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("GitHub");
    }

    @Test
    void forgotByUserId_missingUser_throwsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generateAndSendTokenByUserId(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }

    // ----------------------------------------------------------------
    // isTokenValid
    // ----------------------------------------------------------------

    @Test
    void isTokenValid_activeToken_returnsTrue() {
        User staff = staffUser(1L, "a@b.com");
        when(tokenRepository.findByTokenAndUsedFalse("valid-token"))
                .thenReturn(Optional.of(activeToken(staff, "valid-token")));

        assertThat(service.isTokenValid("valid-token")).isTrue();
    }

    @Test
    void isTokenValid_expiredToken_returnsFalse() {
        User staff = staffUser(1L, "a@b.com");
        when(tokenRepository.findByTokenAndUsedFalse("exp-token"))
                .thenReturn(Optional.of(expiredToken(staff, "exp-token")));

        assertThat(service.isTokenValid("exp-token")).isFalse();
    }

    @Test
    void isTokenValid_unknownToken_returnsFalse() {
        when(tokenRepository.findByTokenAndUsedFalse("unknown")).thenReturn(Optional.empty());

        assertThat(service.isTokenValid("unknown")).isFalse();
    }

    @Test
    void isTokenValid_usedToken_notReturnedByRepo_returnsFalse() {
        // findByTokenAndUsedFalse zaten used=false olmayanları filtreler
        when(tokenRepository.findByTokenAndUsedFalse("used-token")).thenReturn(Optional.empty());

        assertThat(service.isTokenValid("used-token")).isFalse();
    }

    // ----------------------------------------------------------------
    // updatePassword
    // ----------------------------------------------------------------

    @Test
    void updatePassword_validToken_encodesAndSavesAndMarksUsed() {
        User staff = staffUser(2L, "x@y.com");
        PasswordResetToken token = activeToken(staff, "tok-abc");
        when(tokenRepository.findByToken("tok-abc")).thenReturn(Optional.of(token));

        service.updatePassword("tok-abc", "newPass99");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("ENCODED_newPass99");

        assertThat(token.isUsed()).isTrue();
        verify(tokenRepository).save(token);
    }

    @Test
    void updatePassword_expiredToken_throwsExpiredMessage() {
        User staff = staffUser(2L, "x@y.com");
        PasswordResetToken token = expiredToken(staff, "expired-tok");
        when(tokenRepository.findByToken("expired-tok")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.updatePassword("expired-tok", "newPass99"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void updatePassword_alreadyUsedToken_throwsUsedMessage() {
        User staff = staffUser(2L, "x@y.com");
        PasswordResetToken token = usedToken(staff, "used-tok");
        when(tokenRepository.findByToken("used-tok")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.updatePassword("used-tok", "newPass99"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already been used");
    }

    @Test
    void updatePassword_unknownToken_throwsInvalidMessage() {
        when(tokenRepository.findByToken("no-such-tok")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updatePassword("no-such-tok", "newPass99"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid");
    }

    @Test
    void updatePassword_blankToken_throwsImmediately() {
        assertThatThrownBy(() -> service.updatePassword("  ", "newPass99"))
                .isInstanceOf(RuntimeException.class);

        verifyNoInteractions(tokenRepository, userRepository);
    }

    @Test
    void updatePassword_shortPassword_throwsValidationError() {
        assertThatThrownBy(() -> service.updatePassword("tok", "12345"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("6 characters");

        verifyNoInteractions(tokenRepository, userRepository);
    }

    @Test
    void updatePassword_tokenCannotBeReusedTwice() {
        User staff = staffUser(3L, "reuse@test.com");
        PasswordResetToken token = activeToken(staff, "reuse-tok");
        when(tokenRepository.findByToken("reuse-tok")).thenReturn(Optional.of(token));

        // İlk kullanım başarılı
        service.updatePassword("reuse-tok", "firstPass1");
        assertThat(token.isUsed()).isTrue();

        // Token artık used=true, ikinci kullanım hata vermeli
        assertThatThrownBy(() -> service.updatePassword("reuse-tok", "secondPass2"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already been used");
    }
}
