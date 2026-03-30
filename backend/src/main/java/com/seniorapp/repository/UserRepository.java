package com.seniorapp.repository;

import com.seniorapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByGithubId(Long githubId);

    boolean existsByEmail(String email);
}
