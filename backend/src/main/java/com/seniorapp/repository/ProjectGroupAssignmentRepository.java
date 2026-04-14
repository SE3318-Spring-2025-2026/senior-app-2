package com.seniorapp.repository;

import com.seniorapp.entity.ProjectGroupAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectGroupAssignmentRepository extends JpaRepository<ProjectGroupAssignment, Long> {
    Optional<ProjectGroupAssignment> findByProjectIdAndActiveTrue(Long projectId);
}
