package com.seniorapp.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "project_student_story_points",
        uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "student_user_id", "sprint_no"}))
public class ProjectStudentStoryPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "student_user_id", nullable = false)
    private Long studentUserId;

    @Column(name = "sprint_no")
    private Integer sprintNo;

    /** Advisor / koordinatör tarafından girilen manuel story point; null = henüz yok. */
    @Column(name = "story_points")
    private Double storyPoints;

    @Column(name = "accepted", nullable = false)
    private boolean accepted = false;

    @Column(name = "accepted_by_user_id")
    private Long acceptedByUserId;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "updated_by_user_id")
    private Long updatedByUserId;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public Long getStudentUserId() {
        return studentUserId;
    }

    public void setStudentUserId(Long studentUserId) {
        this.studentUserId = studentUserId;
    }

    public Double getStoryPoints() {
        return storyPoints;
    }

    public void setStoryPoints(Double storyPoints) {
        this.storyPoints = storyPoints;
    }

    public Integer getSprintNo() {
        return sprintNo;
    }

    public void setSprintNo(Integer sprintNo) {
        this.sprintNo = sprintNo;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }

    public Long getAcceptedByUserId() {
        return acceptedByUserId;
    }

    public void setAcceptedByUserId(Long acceptedByUserId) {
        this.acceptedByUserId = acceptedByUserId;
    }

    public LocalDateTime getAcceptedAt() {
        return acceptedAt;
    }

    public void setAcceptedAt(LocalDateTime acceptedAt) {
        this.acceptedAt = acceptedAt;
    }

    public Long getUpdatedByUserId() {
        return updatedByUserId;
    }

    public void setUpdatedByUserId(Long updatedByUserId) {
        this.updatedByUserId = updatedByUserId;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
