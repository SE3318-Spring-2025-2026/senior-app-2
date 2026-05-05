package com.seniorapp.repository;

import com.seniorapp.entity.ProjectEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProjectEvaluationRepository extends JpaRepository<ProjectEvaluation, Long> {

    @Query(
            "select distinct e from ProjectEvaluation e join fetch e.sprint s join fetch s.project p where e.id = :id")
    Optional<ProjectEvaluation> findByIdWithProjectChain(@Param("id") Long id);
}
