package com.seniorapp.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "project_issue_sync_snapshots",
        uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "issue_key"}))
public class ProjectIssueSyncSnapshot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "issue_key", nullable = false, length = 100)
    private String issueKey;

    @Column(name = "issue_title", length = 500)
    private String issueTitle;

    @Column(name = "sprint_no")
    private Integer sprintNo;

    @Column(name = "work_type", length = 255)
    private String workType;

    @Column(length = 255)
    private String assignee;

    @Column(length = 255)
    private String reporter;

    @Column(length = 255)
    private String resolution;

    @Column(name = "created_remote")
    private LocalDateTime createdRemote;

    @Column(name = "updated_remote")
    private LocalDateTime updatedRemote;

    @Column(name = "issue_description", columnDefinition = "TEXT")
    private String issueDescription;

    @Column(name = "story_points")
    private Double storyPoints;

    @Column(name = "branch_name", length = 255)
    private String branchName;

    @Column(name = "pr_number")
    private Integer prNumber;

    @Column(name = "pr_merged")
    private Boolean prMerged;

    @Column(name = "synced_at", nullable = false)
    private LocalDateTime syncedAt;

    @PrePersist
    @PreUpdate
    void onSync() {
        syncedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public String getIssueKey() { return issueKey; }
    public void setIssueKey(String issueKey) { this.issueKey = issueKey; }
    public String getIssueTitle() { return issueTitle; }
    public void setIssueTitle(String issueTitle) { this.issueTitle = issueTitle; }
    public Integer getSprintNo() { return sprintNo; }
    public void setSprintNo(Integer sprintNo) { this.sprintNo = sprintNo; }
    public String getWorkType() { return workType; }
    public void setWorkType(String workType) { this.workType = workType; }
    public String getAssignee() { return assignee; }
    public void setAssignee(String assignee) { this.assignee = assignee; }
    public String getReporter() { return reporter; }
    public void setReporter(String reporter) { this.reporter = reporter; }
    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }
    public LocalDateTime getCreatedRemote() { return createdRemote; }
    public void setCreatedRemote(LocalDateTime createdRemote) { this.createdRemote = createdRemote; }
    public LocalDateTime getUpdatedRemote() { return updatedRemote; }
    public void setUpdatedRemote(LocalDateTime updatedRemote) { this.updatedRemote = updatedRemote; }
    public String getIssueDescription() { return issueDescription; }
    public void setIssueDescription(String issueDescription) { this.issueDescription = issueDescription; }
    public Double getStoryPoints() { return storyPoints; }
    public void setStoryPoints(Double storyPoints) { this.storyPoints = storyPoints; }
    public String getBranchName() { return branchName; }
    public void setBranchName(String branchName) { this.branchName = branchName; }
    public Integer getPrNumber() { return prNumber; }
    public void setPrNumber(Integer prNumber) { this.prNumber = prNumber; }
    public Boolean getPrMerged() { return prMerged; }
    public void setPrMerged(Boolean prMerged) { this.prMerged = prMerged; }
    public LocalDateTime getSyncedAt() { return syncedAt; }
    public void setSyncedAt(LocalDateTime syncedAt) { this.syncedAt = syncedAt; }
}
