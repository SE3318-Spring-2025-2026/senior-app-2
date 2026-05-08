package com.seniorapp.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Coordinator-set story point target for a specific student in a specific sprint.
 *
 * <p>Example: Sprint 1, Student 11070001000 → target 5 story points.
 * The actual completed points are stored in {@link ProjectStudentStoryPoint}.
 *
 * <p>The combination (sprint, studentUserId) is unique.
 */
@Entity
@Table(
        name = "sprint_student_sp_targets",
        uniqueConstraints = @UniqueConstraint(columnNames = {"sprint_id", "student_user_id"})
)
public class SprintStudentStoryPointTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id", nullable = false)
    private ProjectSprint sprint;

    @Column(name = "student_user_id", nullable = false)
    private Long studentUserId;

    /** Target story points, must be >= 0. */
    @Column(name = "target_points", nullable = false)
    private Integer targetPoints;

    /** User ID of the coordinator who set this target. */
    @Column(name = "set_by_user_id", nullable = false)
    private Long setByUserId;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ProjectSprint getSprint() { return sprint; }
    public void setSprint(ProjectSprint sprint) { this.sprint = sprint; }

    public Long getStudentUserId() { return studentUserId; }
    public void setStudentUserId(Long studentUserId) { this.studentUserId = studentUserId; }

    public Integer getTargetPoints() { return targetPoints; }
    public void setTargetPoints(Integer targetPoints) { this.targetPoints = targetPoints; }

    public Long getSetByUserId() { return setByUserId; }
    public void setSetByUserId(Long setByUserId) { this.setByUserId = setByUserId; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
