package com.seniorapp.repository;

import com.seniorapp.entity.PasswordResetToken;
import com.seniorapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    Optional<PasswordResetToken> findByTokenAndUsedFalse(String token);

    List<PasswordResetToken> findByUserAndUsedFalse(User user);
}