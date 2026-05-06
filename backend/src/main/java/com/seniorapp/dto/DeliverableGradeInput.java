package com.seniorapp.dto;

import java.util.Map;

/**
 * Represents a graded deliverable with its committee-assigned score and associated scalar.
 */
public class DeliverableGradeInput {
    
    /**
     * Name of the deliverable (e.g., "Proposal", "SoW", "Demonstration")
     */
    private String deliverableName;
    
    /**
     * Committee-assigned grade for this deliverable (typically 0-100)
     */
    private Double committeeGrade;
    
    /**
     * The scalar (coefficient) for this deliverable, calculated from advisor grades
     */
    private Double scalar;
    
    /**
     * Weight of this deliverable in the final grade calculation as a percentage (0-100)
     */
    private Double weight;

    public DeliverableGradeInput() {}

    public DeliverableGradeInput(String deliverableName, Double committeeGrade, Double scalar, Double weight) {
        this.deliverableName = deliverableName;
        this.committeeGrade = committeeGrade;
        this.scalar = scalar;
        this.weight = weight;
    }

    public String getDeliverableName() {
        return deliverableName;
    }

    public void setDeliverableName(String deliverableName) {
        this.deliverableName = deliverableName;
    }

    public Double getCommitteeGrade() {
        return committeeGrade;
    }

    public void setCommitteeGrade(Double committeeGrade) {
        this.committeeGrade = committeeGrade;
    }

    public Double getScalar() {
        return scalar;
    }

    public void setScalar(Double scalar) {
        this.scalar = scalar;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }
}
