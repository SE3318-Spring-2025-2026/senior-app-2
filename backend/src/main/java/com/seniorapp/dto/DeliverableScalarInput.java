package com.seniorapp.dto;

import java.util.List;

/**
 * Represents input data for calculating a deliverable's scalar (coefficient).
 * A scalar is the average of advisor grades (Point A and B) across all sprints
 * that contribute to this deliverable.
 */
public class DeliverableScalarInput {
    
    /**
     * List of Point A grades (team performance) from advisors across sprints.
     * Values are typically from Soft Grading: 100, 80, 60, 50, 0
     */
    private List<Double> pointAGrades;
    
    /**
     * List of Point B grades (code review) from advisors across sprints.
     * Values are typically from Soft Grading: 100, 80, 60, 50, 0
     */
    private List<Double> pointBGrades;
    
    /**
     * Name/identifier of the deliverable (e.g., "Proposal", "SoW", "Demonstration")
     */
    private String deliverableName;

    public DeliverableScalarInput() {}

    public DeliverableScalarInput(List<Double> pointAGrades, List<Double> pointBGrades, String deliverableName) {
        this.pointAGrades = pointAGrades;
        this.pointBGrades = pointBGrades;
        this.deliverableName = deliverableName;
    }

    public List<Double> getPointAGrades() {
        return pointAGrades;
    }

    public void setPointAGrades(List<Double> pointAGrades) {
        this.pointAGrades = pointAGrades;
    }

    public List<Double> getPointBGrades() {
        return pointBGrades;
    }

    public void setPointBGrades(List<Double> pointBGrades) {
        this.pointBGrades = pointBGrades;
    }

    public String getDeliverableName() {
        return deliverableName;
    }

    public void setDeliverableName(String deliverableName) {
        this.deliverableName = deliverableName;
    }
}
