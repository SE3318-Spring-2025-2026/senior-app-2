package com.seniorapp.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class ScoreOverrideRequest {

    @NotNull(message = "Student ID cannot be null")
    private Long studentId; // Projendeki ID tipine göre Long veya UUID yapabilirsin

    @NotNull(message = "Score cannot be null")
    @Min(value = 0, message = "Score cannot be less than 0")
    @Max(value = 100, message = "Score cannot be greater than 100")
    private Double score;

    // Getters and Setters
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public Double getScore() { return score; }
    public void setScore(Double score) { this.score = score; }
}