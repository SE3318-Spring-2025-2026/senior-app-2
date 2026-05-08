package com.seniorapp.repository;

import com.seniorapp.entity.ProjectGithubIssue;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectGithubIssueRepository extends JpaRepository<ProjectGithubIssue, Long> {
    List<ProjectGithubIssue> findByProject_IdOrderByIssueNumberAsc(Long projectId);
    Optional<ProjectGithubIssue> findByProject_IdAndIssueNumber(Long projectId, Long issueNumber);
}
