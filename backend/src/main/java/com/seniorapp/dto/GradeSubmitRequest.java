package com.seniorapp.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public class GradeSubmitRequest {

    @NotNull(message = "Grader ID cannot be null")
    private Long graderId;

    @NotNull(message = "Rubric ID cannot be null")
    private Long rubricId;

    @NotNull(message = "Grade cannot be null")
    @PositiveOrZero(message = "Grade must be positive or zero")
    private Double grade;

    public Long getGraderId() {
        return graderId;
    }

    public void setGraderId(Long graderId) {
        this.graderId = graderId;
    }

    public Long getRubricId() {
        return rubricId;
    }

    public void setRubricId(Long rubricId) {
        this.rubricId = rubricId;
    }

    public Double getGrade() {
        return grade;
    }

    public void setGrade(Double grade) {
        this.grade = grade;
    }
}
