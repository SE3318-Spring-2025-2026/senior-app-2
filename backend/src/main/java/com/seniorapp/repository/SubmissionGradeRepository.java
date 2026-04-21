package com.seniorapp.repository;

import com.seniorapp.entity.SubmissionGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubmissionGradeRepository extends JpaRepository<SubmissionGrade, Long> {
    List<SubmissionGrade> findBySubmissionId(Long submissionId);
    boolean existsBySubmissionIdAndGraderIdAndRubricId(Long submissionId, Long graderId, Long rubricId);
}
