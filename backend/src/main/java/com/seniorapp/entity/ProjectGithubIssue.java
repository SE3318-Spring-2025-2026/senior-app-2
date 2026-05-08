package com.seniorapp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "project_github_issues",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"project_id", "issue_number"})})
public class ProjectGithubIssue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id")
    private ProjectSprint sprint;

    @Column(name = "issue_number", nullable = false)
    private Long issueNumber;

    @Column(nullable = false, length = 1024)
    private String title;

    @Column(nullable = false, length = 32)
    private String state;

    @Column(length = 255)
    private String assignee;

    @Column(name = "story_points")
    private Double storyPoints;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public ProjectSprint getSprint() { return sprint; }
    public void setSprint(ProjectSprint sprint) { this.sprint = sprint; }
    public Long getIssueNumber() { return issueNumber; }
    public void setIssueNumber(Long issueNumber) { this.issueNumber = issueNumber; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getAssignee() { return assignee; }
    public void setAssignee(String assignee) { this.assignee = assignee; }
    public Double getStoryPoints() { return storyPoints; }
    public void setStoryPoints(Double storyPoints) { this.storyPoints = storyPoints; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
