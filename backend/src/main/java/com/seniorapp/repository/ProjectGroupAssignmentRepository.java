package com.seniorapp.repository;

import com.seniorapp.entity.ProjectGroupAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProjectGroupAssignmentRepository extends JpaRepository<ProjectGroupAssignment, Long> {
    Optional<ProjectGroupAssignment> findByProjectIdAndActiveTrue(Long projectId);
    Optional<ProjectGroupAssignment> findByGroupIdAndActiveTrue(Long groupId);
    List<ProjectGroupAssignment> findByGroupIdInAndActiveTrue(List<Long> groupIds);
    long countByProjectIdAndCommittee_IdAndActiveTrue(Long projectId, Long committeeId);

    @Query("select a from ProjectGroupAssignment a where a.groupId = :groupId and a.active = true order by a.id desc")
    List<ProjectGroupAssignment> findActiveByGroupIdOrderByLatest(Long groupId);
}
