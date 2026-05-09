package com.seniorapp.dto;

import com.seniorapp.entity.SubmissionGrade;

public class GradeResponse {
    private Long id;
    private Long submissionId;
    private Long graderId;
    private String graderName;
    private String graderEmail;
    private Long rubricId;
    private Double grade;
    private String comment;

    public GradeResponse() {}

    public GradeResponse(SubmissionGrade entity) {
        this.id = entity.getId();
        if (entity.getSubmission() != null) {
            this.submissionId = entity.getSubmission().getId();
        }
        if (entity.getGrader() != null) {
            this.graderId = entity.getGrader().getId();
            this.graderName = entity.getGrader().getFullName();
            this.graderEmail = entity.getGrader().getEmail();
        }
        this.rubricId = entity.getRubricId();
        this.grade = entity.getGrade();
        this.comment = entity.getComment();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getSubmissionId() { return submissionId; }
    public void setSubmissionId(Long submissionId) { this.submissionId = submissionId; }
    
    public Long getGraderId() { return graderId; }
    public void setGraderId(Long graderId) { this.graderId = graderId; }
    
    public String getGraderName() { return graderName; }
    public void setGraderName(String graderName) { this.graderName = graderName; }

    public String getGraderEmail() { return graderEmail; }
    public void setGraderEmail(String graderEmail) { this.graderEmail = graderEmail; }

    public Long getRubricId() { return rubricId; }
    public void setRubricId(Long rubricId) { this.rubricId = rubricId; }

    public Double getGrade() { return grade; }
    public void setGrade(Double grade) { this.grade = grade; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
