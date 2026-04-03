package com.seniorapp.repository;

import com.seniorapp.entity.StudentWhitelist;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StudentWhitelistRepository extends JpaRepository<StudentWhitelist, Long> {
    Optional<StudentWhitelist> findByStudentId(String studentId);
    boolean existsByStudentId(String studentId);
}