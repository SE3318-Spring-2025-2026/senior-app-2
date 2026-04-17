package com.seniorapp.dto;

import com.seniorapp.entity.SubmissionGrade;

public class GradeResponse {
    private Long id;
    private Long submissionId;
    private Long graderId;
    private Long rubricId;
    private Double grade;

    public GradeResponse() {}

    public GradeResponse(SubmissionGrade entity) {
        this.id = entity.getId();
        if (entity.getSubmission() != null) {
            this.submissionId = entity.getSubmission().getId();
        }
        if (entity.getGrader() != null) {
            this.graderId = entity.getGrader().getId();
        }
        this.rubricId = entity.getRubricId();
        this.grade = entity.getGrade();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getSubmissionId() { return submissionId; }
    public void setSubmissionId(Long submissionId) { this.submissionId = submissionId; }
    
    public Long getGraderId() { return graderId; }
    public void setGraderId(Long graderId) { this.graderId = graderId; }

    public Long getRubricId() { return rubricId; }
    public void setRubricId(Long rubricId) { this.rubricId = rubricId; }

    public Double getGrade() { return grade; }
    public void setGrade(Double grade) { this.grade = grade; }
}
