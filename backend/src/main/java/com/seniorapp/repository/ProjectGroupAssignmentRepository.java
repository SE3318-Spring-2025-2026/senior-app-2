package com.seniorapp.repository;

import com.seniorapp.entity.ProjectGroupAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectGroupAssignmentRepository extends JpaRepository<ProjectGroupAssignment, Long> {
    Optional<ProjectGroupAssignment> findByProjectIdAndActiveTrue(Long projectId);
    Optional<ProjectGroupAssignment> findByGroupIdAndActiveTrue(Long groupId);
    Optional<ProjectGroupAssignment> findByProject_IdAndGroupIdAndActiveTrue(Long projectId, Long groupId);
    List<ProjectGroupAssignment> findByGroupIdInAndActiveTrue(List<Long> groupIds);
    List<ProjectGroupAssignment> findByProject_IdAndActiveTrue(Long projectId);
}
