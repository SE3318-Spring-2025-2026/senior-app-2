package com.seniorapp.repository;

import com.seniorapp.entity.User;
import com.seniorapp.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link User} entities.
 * Provides lookup methods beyond the standard JPA operations.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /** Find a user by their e-mail address. */
    Optional<User> findByEmail(String email);

    /** Find a user by the numeric GitHub account ID. */
    Optional<User> findByGithubId(Long githubId);

    /** Find a user by their GitHub login name (username). */
    Optional<User> findByGithubUsername(String githubUsername);

    /** Check whether an e-mail is already registered. */
    boolean existsByEmail(String email);

    List<User> findByRole(Role role);
}
