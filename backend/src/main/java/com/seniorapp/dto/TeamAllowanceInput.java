package com.seniorapp.dto;

/**
 * Represents input data for calculating team allowance (coefficient).
 * Team allowance is the average of Point A (team performance) and Point B (code review) grades.
 */
public class TeamAllowanceInput {
    
    /**
     * Average of Point A grades (advisor's assessment of team performance)
     * Typically calculated from Soft Grading values: 100, 80, 60, 50, 0
     */
    private Double avgPointA;
    
    /**
     * Average of Point B grades (advisor's assessment of code/work review)
     * Typically calculated from Soft Grading values: 100, 80, 60, 50, 0
     */
    private Double avgPointB;
    
    /**
     * Team identifier if needed for logging/debugging
     */
    private Long teamId;

    public TeamAllowanceInput() {}

    public TeamAllowanceInput(Double avgPointA, Double avgPointB) {
        this.avgPointA = avgPointA;
        this.avgPointB = avgPointB;
    }

    public TeamAllowanceInput(Double avgPointA, Double avgPointB, Long teamId) {
        this.avgPointA = avgPointA;
        this.avgPointB = avgPointB;
        this.teamId = teamId;
    }

    public Double getAvgPointA() {
        return avgPointA;
    }

    public void setAvgPointA(Double avgPointA) {
        this.avgPointA = avgPointA;
    }

    public Double getAvgPointB() {
        return avgPointB;
    }

    public void setAvgPointB(Double avgPointB) {
        this.avgPointB = avgPointB;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }
}
