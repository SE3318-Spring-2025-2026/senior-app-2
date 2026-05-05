package com.seniorapp.repository;

import com.seniorapp.entity.ProjectEvaluationRubric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProjectEvaluationRubricRepository extends JpaRepository<ProjectEvaluationRubric, Long> {

    List<ProjectEvaluationRubric> findByEvaluation_IdIn(Collection<Long> evaluationIds);

    @Query(
            "select distinct r from ProjectEvaluationRubric r join fetch r.evaluation e join fetch e.sprint s join fetch s.project p "
                    + "where r.id = :id")
    Optional<ProjectEvaluationRubric> findByIdWithProjectChain(@Param("id") Long id);
}
