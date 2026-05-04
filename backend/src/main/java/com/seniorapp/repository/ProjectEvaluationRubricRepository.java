package com.seniorapp.repository;

import com.seniorapp.entity.ProjectEvaluationRubric;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ProjectEvaluationRubricRepository extends JpaRepository<ProjectEvaluationRubric, Long> {

    List<ProjectEvaluationRubric> findByEvaluation_IdIn(Collection<Long> evaluationIds);
}
