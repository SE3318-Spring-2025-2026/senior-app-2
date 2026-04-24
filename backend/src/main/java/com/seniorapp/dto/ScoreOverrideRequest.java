package com.seniorapp.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

@Data
public class ScoreOverrideRequest {
    @NotNull(message = "studentId is required")
    private String studentId;
    
    @NotNull(message = "manualScore is required")
    private Double manualScore;
}