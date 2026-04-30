package com.seniorapp.repository;

import com.seniorapp.entity.SubmissionGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubmissionGradeRepository extends JpaRepository<SubmissionGrade, Long> {
    List<SubmissionGrade> findBySubmissionId(Long submissionId);
    boolean existsBySubmissionIdAndGraderIdAndRubricId(Long submissionId, Long graderId, Long rubricId);

    @Query("select avg(g.grade) from SubmissionGrade g where g.submission.id = :submissionId")
    Double findAverageGradeBySubmissionId(Long submissionId);
}
