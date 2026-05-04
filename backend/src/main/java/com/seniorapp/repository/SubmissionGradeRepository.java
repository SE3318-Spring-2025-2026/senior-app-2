package com.seniorapp.repository;

import com.seniorapp.entity.SubmissionGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionGradeRepository extends JpaRepository<SubmissionGrade, Long> {
    /**
     * Grader lazy kaldığında DTO oluşturulurken graderId null kalıp istemci filtresi tüm satırları atabiliyordu.
     */
    @Query("select g from SubmissionGrade g join fetch g.grader where g.submission.id = :submissionId")
    List<SubmissionGrade> findAllForSubmission(@Param("submissionId") Long submissionId);
    boolean existsBySubmissionIdAndGraderIdAndRubricId(Long submissionId, Long graderId, Long rubricId);

    Optional<SubmissionGrade> findBySubmission_IdAndGrader_IdAndRubricId(Long submissionId, Long graderId, Long rubricId);
}
