package com.seniorapp.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public class GradeSubmitRequest {

    @NotNull(message = "Rubric ID cannot be null")
    private Long rubricId;

    @NotNull(message = "Grade cannot be null")
    @PositiveOrZero(message = "Grade must be positive or zero")
    private Double grade;

    /** Optional grader feedback; persisted on submission_grades.comment */
    private String comment;

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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
