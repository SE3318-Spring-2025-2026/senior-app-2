package com.seniorapp.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Stores the soft-grade an advisor gives a group for a specific sprint,
 * for either the Scrum dimension (Point A) or the Code Review dimension (Point B).
 *
 * <p>Soft-grade letter values map as follows (per spec):
 * A = 100, B = 80, C = 60, D = 50, F = 0.
 *
 * <p>The combination (sprint, group, gradeType) is unique.
 */
@Entity
@Table(
        name = "sprint_advisor_grades",
        uniqueConstraints = @UniqueConstraint(columnNames = {"sprint_id", "group_id", "grade_type"})
)
public class SprintAdvisorGrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id", nullable = false)
    private ProjectSprint sprint;

    /** The UserGroup being graded. */
    @Column(name = "group_id", nullable = false)
    private Long groupId;

    /** The advisor who submitted the grade. */
    @Column(name = "advisor_user_id", nullable = false)
    private Long advisorUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "grade_type", nullable = false, length = 20)
    private SprintGradeType gradeType;

    /**
     * Soft-grade letter: A, B, C, D, or F.
     * Stored as a character string for clarity.
     */
    @Column(name = "soft_grade", nullable = false, length = 1)
    private String softGrade;

    @Column(length = 2000)
    private String comment;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ── Getters / Setters ──────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ProjectSprint getSprint() { return sprint; }
    public void setSprint(ProjectSprint sprint) { this.sprint = sprint; }

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public Long getAdvisorUserId() { return advisorUserId; }
    public void setAdvisorUserId(Long advisorUserId) { this.advisorUserId = advisorUserId; }

    public SprintGradeType getGradeType() { return gradeType; }
    public void setGradeType(SprintGradeType gradeType) { this.gradeType = gradeType; }

    public String getSoftGrade() { return softGrade; }
    public void setSoftGrade(String softGrade) { this.softGrade = softGrade; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
