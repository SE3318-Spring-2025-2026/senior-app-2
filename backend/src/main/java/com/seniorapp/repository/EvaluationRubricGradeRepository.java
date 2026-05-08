package com.seniorapp.repository;

import com.seniorapp.entity.EvaluationRubricGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EvaluationRubricGradeRepository extends JpaRepository<EvaluationRubricGrade, Long> {

    Optional<EvaluationRubricGrade> findByGroupIdAndGrader_IdAndEvaluationRubric_Id(
            Long groupId, Long graderId, Long evaluationRubricId);

    @Query(
            "select g from EvaluationRubricGrade g join fetch g.grader join fetch g.evaluationRubric er "
                    + "where g.groupId = :groupId and er.evaluation.id = :evaluationId")
    List<EvaluationRubricGrade> findAllForGroupAndEvaluation(
            @Param("groupId") Long groupId, @Param("evaluationId") Long evaluationId);

    @Query(
            "select avg(g.grade) from EvaluationRubricGrade g join g.evaluationRubric er "
                    + "where g.groupId = :groupId and er.evaluation.id = :evaluationId")
    Double averageGradeForGroupAndEvaluation(
            @Param("groupId") Long groupId, @Param("evaluationId") Long evaluationId);
}
