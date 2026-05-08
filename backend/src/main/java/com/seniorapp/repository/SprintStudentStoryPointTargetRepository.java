package com.seniorapp.repository;

import com.seniorapp.entity.SprintStudentStoryPointTarget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SprintStudentStoryPointTargetRepository
        extends JpaRepository<SprintStudentStoryPointTarget, Long> {

    Optional<SprintStudentStoryPointTarget> findBySprintIdAndStudentUserId(Long sprintId, Long studentUserId);

    List<SprintStudentStoryPointTarget> findBySprintId(Long sprintId);

    @Query("""
        SELECT t FROM SprintStudentStoryPointTarget t
        WHERE t.sprint.project.id = :projectId
          AND t.studentUserId = :studentUserId
        ORDER BY t.sprint.sprintNo
    """)
    List<SprintStudentStoryPointTarget> findByProjectIdAndStudentUserId(
            @Param("projectId") Long projectId,
            @Param("studentUserId") Long studentUserId);
}
