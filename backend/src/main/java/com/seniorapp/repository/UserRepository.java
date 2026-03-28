package com.seniorapp.repository;

import com.seniorapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByGithubId(Long githubId);

    Optional<User> findByStudentId(String studentId);

    Optional<User> findByPasswordResetToken(String token);

    boolean existsByEmail(String email);

    boolean existsByStudentId(String studentId);
}
