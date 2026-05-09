package com.seniorapp.repository;

import com.seniorapp.entity.ProjectIssueSyncSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectIssueSyncSnapshotRepository extends JpaRepository<ProjectIssueSyncSnapshot, Long> {
    Optional<ProjectIssueSyncSnapshot> findByProject_IdAndIssueKey(Long projectId, String issueKey);
    Optional<ProjectIssueSyncSnapshot> findByProject_IdAndPrNumber(Long projectId, Integer prNumber);
    List<ProjectIssueSyncSnapshot> findByProject_IdOrderByIssueKeyAsc(Long projectId);
    List<ProjectIssueSyncSnapshot> findByProject_IdAndSprintNoOrderByIssueKeyAsc(Long projectId, Integer sprintNo);
}
