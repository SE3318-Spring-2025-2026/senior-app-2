package com.seniorapp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "submission_grades")
public class SubmissionGrade {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "submission_id", nullable = false)
    private DeliverableSubmission submission;

    @ManyToOne(optional = false)
    @JoinColumn(name = "grader_id", nullable = false)
    private User grader;

    @Column(name = "rubric_id", nullable = false)
    private Long rubricId;

    @Column(nullable = false)
    private Double grade;

    
    @Column(name = "original_ai_score")
    private Double originalAiScore;

    @Column(name = "advisor_adjusted_score")
    private Double advisorAdjustedScore;

    @Column(name = "final_grade_4_0")
    private Double finalGrade40;

    @Column(name = "last_modified_by")
    private String lastModifiedBy;

    @Column(name = "last_modified_at")
    private LocalDateTime lastModifiedAt;

  
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public DeliverableSubmission getSubmission() { return submission; }
    public void setSubmission(DeliverableSubmission submission) { this.submission = submission; }

    public User getGrader() { return grader; }
    public void setGrader(User grader) { this.grader = grader; }

    public Long getRubricId() { return rubricId; }
    public void setRubricId(Long rubricId) { this.rubricId = rubricId; }

    public Double getGrade() { return grade; }
    public void setGrade(Double grade) { this.grade = grade; }

    // --- Added Getters and Setters for Issue #129 ---
    public Double getOriginalAiScore() { return originalAiScore; }
    public void setOriginalAiScore(Double originalAiScore) { this.originalAiScore = originalAiScore; }

    public Double getAdvisorAdjustedScore() { return advisorAdjustedScore; }
    public void setAdvisorAdjustedScore(Double advisorAdjustedScore) { this.advisorAdjustedScore = advisorAdjustedScore; }

    public Double getFinalGrade40() { return finalGrade40; }
    public void setFinalGrade40(Double finalGrade40) { this.finalGrade40 = finalGrade40; }

    public String getLastModifiedBy() { return lastModifiedBy; }
    public void setLastModifiedBy(String lastModifiedBy) { this.lastModifiedBy = lastModifiedBy; }

    public LocalDateTime getLastModifiedAt() { return lastModifiedAt; }
    public void setLastModifiedAt(LocalDateTime lastModifiedAt) { this.lastModifiedAt = lastModifiedAt; }
}