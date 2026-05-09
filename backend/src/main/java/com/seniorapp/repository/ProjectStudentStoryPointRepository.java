package com.seniorapp.repository;

import com.seniorapp.entity.ProjectStudentStoryPoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectStudentStoryPointRepository extends JpaRepository<ProjectStudentStoryPoint, Long> {

    List<ProjectStudentStoryPoint> findByProject_Id(Long projectId);

    Optional<ProjectStudentStoryPoint> findByProject_IdAndStudentUserId(Long projectId, Long studentUserId);

    List<ProjectStudentStoryPoint> findByProject_IdAndSprintNo(Long projectId, Integer sprintNo);

    Optional<ProjectStudentStoryPoint> findByProject_IdAndStudentUserIdAndSprintNo(
            Long projectId, Long studentUserId, Integer sprintNo);
}
