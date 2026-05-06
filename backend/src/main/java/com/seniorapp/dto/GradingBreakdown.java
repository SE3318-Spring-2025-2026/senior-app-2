package com.seniorapp.dto;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents an intermediate breakdown of grading calculations for debugging and transparency.
 * Shows each step of the calculation process.
 */
public class GradingBreakdown {
    
    /**
     * Team allowance (coefficient) calculated from Point A and B
     */
    private Double teamAllowance;
    
    /**
     * Map of deliverable name → scaled grade for each deliverable
     */
    private Map<String, Double> scaledDeliverableGrades;
    
    /**
     * Map of deliverable name → component calculation details
     */
    private Map<String, String> deliverableCalculationDetails;
    
    /**
     * Final team grade (before individual allowance applied)
     */
    private Double teamFinalGrade;
    
    /**
     * Student story point ratio (studentPoints / totalPoints)
     */
    private Double studentStoryPointRatio;
    
    /**
     * Final individual grade
     */
    private Double individualFinalGrade;
    
    /**
     * Validation errors, if any
     */
    private Map<String, String> validationErrors;

    public GradingBreakdown() {
        this.scaledDeliverableGrades = new LinkedHashMap<>();
        this.deliverableCalculationDetails = new LinkedHashMap<>();
        this.validationErrors = new LinkedHashMap<>();
    }

    public Double getTeamAllowance() {
        return teamAllowance;
    }

    public void setTeamAllowance(Double teamAllowance) {
        this.teamAllowance = teamAllowance;
    }

    public Map<String, Double> getScaledDeliverableGrades() {
        return scaledDeliverableGrades;
    }

    public void setScaledDeliverableGrades(Map<String, Double> scaledDeliverableGrades) {
        this.scaledDeliverableGrades = scaledDeliverableGrades;
    }

    public Map<String, String> getDeliverableCalculationDetails() {
        return deliverableCalculationDetails;
    }

    public void setDeliverableCalculationDetails(Map<String, String> deliverableCalculationDetails) {
        this.deliverableCalculationDetails = deliverableCalculationDetails;
    }

    public Double getTeamFinalGrade() {
        return teamFinalGrade;
    }

    public void setTeamFinalGrade(Double teamFinalGrade) {
        this.teamFinalGrade = teamFinalGrade;
    }

    public Double getStudentStoryPointRatio() {
        return studentStoryPointRatio;
    }

    public void setStudentStoryPointRatio(Double studentStoryPointRatio) {
        this.studentStoryPointRatio = studentStoryPointRatio;
    }

    public Double getIndividualFinalGrade() {
        return individualFinalGrade;
    }

    public void setIndividualFinalGrade(Double individualFinalGrade) {
        this.individualFinalGrade = individualFinalGrade;
    }

    public Map<String, String> getValidationErrors() {
        return validationErrors;
    }

    public void setValidationErrors(Map<String, String> validationErrors) {
        this.validationErrors = validationErrors;
    }
    
    public void addValidationError(String key, String error) {
        this.validationErrors.put(key, error);
    }
    
    public void addScaledGrade(String deliverableName, Double scaledGrade) {
        this.scaledDeliverableGrades.put(deliverableName, scaledGrade);
    }
    
    public void addCalculationDetail(String deliverableName, String detail) {
        this.deliverableCalculationDetails.put(deliverableName, detail);
    }
}
