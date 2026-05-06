package com.seniorapp.dto;

import com.seniorapp.entity.EvaluationRubricGrade;

public class EvaluationGradeResponse {
    private Long id;
    private Long groupId;
    private Long evaluationRubricId;
    private Long evaluationId;
    private Long graderId;
    private Double grade;
    private String comment;

    public EvaluationGradeResponse() {}

    public EvaluationGradeResponse(EvaluationRubricGrade entity) {
        this.id = entity.getId();
        this.groupId = entity.getGroupId();
        if (entity.getEvaluationRubric() != null) {
            this.evaluationRubricId = entity.getEvaluationRubric().getId();
            if (entity.getEvaluationRubric().getEvaluation() != null) {
                this.evaluationId = entity.getEvaluationRubric().getEvaluation().getId();
            }
        }
        if (entity.getGrader() != null) {
            this.graderId = entity.getGrader().getId();
        }
        this.grade = entity.getGrade();
        this.comment = entity.getComment();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public Long getEvaluationRubricId() {
        return evaluationRubricId;
    }

    public void setEvaluationRubricId(Long evaluationRubricId) {
        this.evaluationRubricId = evaluationRubricId;
    }

    public Long getEvaluationId() {
        return evaluationId;
    }

    public void setEvaluationId(Long evaluationId) {
        this.evaluationId = evaluationId;
    }

    public Long getGraderId() {
        return graderId;
    }

    public void setGraderId(Long graderId) {
        this.graderId = graderId;
    }

    public Double getGrade() {
        return grade;
    }

    public void setGrade(Double grade) {
        this.grade = grade;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
