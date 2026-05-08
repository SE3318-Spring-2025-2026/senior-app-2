package com.seniorapp.repository;

import com.seniorapp.entity.SprintDeliverableContribution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SprintDeliverableContributionRepository
        extends JpaRepository<SprintDeliverableContribution, Long> {

    List<SprintDeliverableContribution> findBySprintId(Long sprintId);

    Optional<SprintDeliverableContribution> findBySprintIdAndDeliverableName(Long sprintId, String deliverableName);

    /** All contributions for a project (all sprints under that project). */
    @Query("""
        SELECT sdc FROM SprintDeliverableContribution sdc
        WHERE sdc.sprint.project.id = :projectId
        ORDER BY sdc.sprint.sprintNo, sdc.deliverableName
    """)
    List<SprintDeliverableContribution> findByProjectId(@Param("projectId") Long projectId);

    /** All contributions for a deliverable name within a project. */
    @Query("""
        SELECT sdc FROM SprintDeliverableContribution sdc
        WHERE sdc.sprint.project.id = :projectId
          AND sdc.deliverableName = :deliverableName
        ORDER BY sdc.sprint.sprintNo
    """)
    List<SprintDeliverableContribution> findByProjectIdAndDeliverableName(
            @Param("projectId") Long projectId,
            @Param("deliverableName") String deliverableName);

    void deleteBySprintIdAndDeliverableName(Long sprintId, String deliverableName);
}
