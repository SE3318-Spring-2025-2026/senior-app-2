package com.seniorapp.repository;

import com.seniorapp.entity.SprintAdvisorGrade;
import com.seniorapp.entity.SprintGradeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SprintAdvisorGradeRepository extends JpaRepository<SprintAdvisorGrade, Long> {

    Optional<SprintAdvisorGrade> findBySprintIdAndGroupIdAndGradeType(
            Long sprintId, Long groupId, SprintGradeType gradeType);

    List<SprintAdvisorGrade> findBySprintIdAndGroupId(Long sprintId, Long groupId);

    /** All grades for a group across all sprints in a project. */
    @Query("""
        SELECT sag FROM SprintAdvisorGrade sag
        WHERE sag.sprint.project.id = :projectId
          AND sag.groupId = :groupId
        ORDER BY sag.sprint.sprintNo, sag.gradeType
    """)
    List<SprintAdvisorGrade> findByProjectIdAndGroupId(
            @Param("projectId") Long projectId,
            @Param("groupId") Long groupId);

    /** All grades for a project for computing scalars. */
    @Query("""
        SELECT sag FROM SprintAdvisorGrade sag
        WHERE sag.sprint.project.id = :projectId
        ORDER BY sag.sprint.sprintNo, sag.gradeType
    """)
    List<SprintAdvisorGrade> findByProjectId(@Param("projectId") Long projectId);
}
