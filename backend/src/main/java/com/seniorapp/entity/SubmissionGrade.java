package com.seniorapp.entity;

import jakarta.persistence.*;

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
}
