package com.seniorapp.dto;

/**
 * Represents the complete result of grading calculations.
 * Contains both the final grade and detailed breakdown for transparency and debugging.
 */
public class GradingResult {
    
    /**
     * Final calculated grade
     */
    private Double finalGrade;
    
    /**
     * Success indicator - true if calculation completed without errors
     */
    private Boolean success;
    
    /**
     * Detailed breakdown of all calculation steps
     */
    private GradingBreakdown breakdown;
    
    /**
     * Error message if calculation failed
     */
    private String errorMessage;

    public GradingResult() {
        this.success = true;
        this.breakdown = new GradingBreakdown();
    }

    public GradingResult(Double finalGrade, GradingBreakdown breakdown) {
        this.finalGrade = finalGrade;
        this.success = true;
        this.breakdown = breakdown;
    }

    public GradingResult(Boolean success, String errorMessage) {
        this.success = success;
        this.errorMessage = errorMessage;
        this.breakdown = new GradingBreakdown();
    }

    public Double getFinalGrade() {
        return finalGrade;
    }

    public void setFinalGrade(Double finalGrade) {
        this.finalGrade = finalGrade;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public GradingBreakdown getBreakdown() {
        return breakdown;
    }

    public void setBreakdown(GradingBreakdown breakdown) {
        this.breakdown = breakdown;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
