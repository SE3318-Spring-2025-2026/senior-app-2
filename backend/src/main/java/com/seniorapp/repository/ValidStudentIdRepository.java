package com.seniorapp.repository;

import com.seniorapp.entity.ValidStudentId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link ValidStudentId} entities.
 * Used to validate whether a student identity (e-mail or ID string)
 * has been pre-approved by a Coordinator before account creation.
 */
public interface ValidStudentIdRepository extends JpaRepository<ValidStudentId, Long> {

    /** Find a whitelist entry by the student ID / e-mail string. */
    Optional<ValidStudentId> findByStudentId(String studentId);

    /** Return {@code true} if the given student ID is on the whitelist. */
    boolean existsByStudentId(String studentId);

    /** Return all entries that have not yet been linked to a User account. */
    List<ValidStudentId> findByAccountIsNull();

    /** Return all entries that have already been linked to a User account. */
    List<ValidStudentId> findByAccountIsNotNull();
}
