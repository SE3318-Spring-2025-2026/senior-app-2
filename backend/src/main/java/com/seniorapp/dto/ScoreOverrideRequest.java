package com.seniorapp.dto;

import lombok.Data;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Data
public class ScoreOverrideRequest {

    @NotNull(message = "studentId is required")
    private Long studentId; 
    
    @NotNull(message = "Score cannot be null")
    @Min(value = 0, message = "Score cannot be less than 0")
    @Max(value = 100, message = "Score cannot be greater than 100")
    private Double score;
}