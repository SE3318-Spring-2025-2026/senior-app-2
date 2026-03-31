package com.seniorapp.repository;

import com.seniorapp.entity.ValidStudentId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ValidStudentIdRepository extends JpaRepository<ValidStudentId, Long> {

    Optional<ValidStudentId> findByStudentId(String studentId);

    boolean existsByStudentId(String studentId);

    List<ValidStudentId> findByAccountIsNull();

    List<ValidStudentId> findByAccountIsNotNull();
}
