package com.seniorapp.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** Body for POST evaluation rubric grade (rubric id is in the path). */
public class EvaluationGradeSubmitRequest {

    @NotNull(message = "Grade cannot be null")
    @PositiveOrZero(message = "Grade must be positive or zero")
    private Double grade;

    private String comment;

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
