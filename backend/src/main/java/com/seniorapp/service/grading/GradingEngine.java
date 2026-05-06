package com.seniorapp.service.grading;

import com.seniorapp.dto.*;
import java.util.*;

/**
 * Core grading engine - pure business logic for calculating grades.
 * This service is API-agnostic and contains no dependencies on HTTP, controllers, or database.
 * It only performs calculations based on provided data and validates inputs defensively.
 * 
 * Grading Formula:
 * 1. calculateDeliverableScalar: AVG(Point A and B grades per deliverable)
 * 2. calculateTeamAllowance: AVG(Point A, Point B) / 100
 * 3. calculateScaledDeliverableGrade: Committee Grade × Deliverable Scalar
 * 4. calculateTeamFinalGrade: Weighted sum of all scaled deliverable grades
 * 5. calculateIndividualGrade: Team Final Grade × (Student Story Points / Total Story Points)
 */
public class GradingEngine {

    /**
     * Precision for rounding calculations (2 decimal places = 0.01)
     */
    private static final int DECIMAL_PRECISION = 2;

    /**
     * Calculates the scalar (coefficient) for a deliverable based on advisor grades.
     * Scalar = AVG(Point A grades + Point B grades)
     * 
     * @param input Contains lists of Point A and B grades for the deliverable
     * @return GradingResult with scalar value (0.0 to 1.0 typically)
     */
    public GradingResult calculateDeliverableScalar(DeliverableScalarInput input) {
        GradingResult result = new GradingResult();
        
        try {
            // Defensive validation
            if (input == null) {
                throw new IllegalArgumentException("DeliverableScalarInput cannot be null");
            }
            if (input.getPointAGrades() == null || input.getPointAGrades().isEmpty()) {
                throw new IllegalArgumentException("Point A grades list cannot be null or empty");
            }
            if (input.getPointBGrades() == null || input.getPointBGrades().isEmpty()) {
                throw new IllegalArgumentException("Point B grades list cannot be null or empty");
            }
            if (input.getPointAGrades().size() != input.getPointBGrades().size()) {
                throw new IllegalArgumentException("Point A and Point B grades must have the same number of entries");
            }
            
            // Validate grade values
            validateGradeValues(input.getPointAGrades(), "Point A");
            validateGradeValues(input.getPointBGrades(), "Point B");
            
            // Calculate average
            double avgPointA = calculateAverage(input.getPointAGrades());
            double avgPointB = calculateAverage(input.getPointBGrades());
            double allGrades = (avgPointA + avgPointB) / 2;
            
            // Convert to scalar (divide by 100)
            double scalar = allGrades / 100.0;
            scalar = roundToDecimalPrecision(scalar);
            
            result.setFinalGrade(scalar);
            result.getBreakdown().setTeamAllowance(scalar);
            result.getBreakdown().addCalculationDetail(
                input.getDeliverableName() != null ? input.getDeliverableName() : "Deliverable",
                String.format("AVG(Point A: %.2f, Point B: %.2f) = %.2f → Scalar: %.4f", 
                    avgPointA, avgPointB, allGrades, scalar)
            );
            
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage("Error calculating deliverable scalar: " + e.getMessage());
            result.getBreakdown().addValidationError("scalar_calculation", e.getMessage());
        }
        
        return result;
    }

    /**
     * Calculates team allowance (coefficient) from Point A and B averages.
     * Team Allowance = AVG(Point A, Point B) / 100
     * 
     * @param input Contains average Point A and B grades
     * @return GradingResult with team allowance value (0.0 to 1.0 typically)
     */
    public GradingResult calculateTeamAllowance(TeamAllowanceInput input) {
        GradingResult result = new GradingResult();
        
        try {
            // Defensive validation
            if (input == null) {
                throw new IllegalArgumentException("TeamAllowanceInput cannot be null");
            }
            if (input.getAvgPointA() == null) {
                throw new IllegalArgumentException("Average Point A grade cannot be null");
            }
            if (input.getAvgPointB() == null) {
                throw new IllegalArgumentException("Average Point B grade cannot be null");
            }
            
            // Validate grade values (should be 0-100)
            if (input.getAvgPointA() < 0 || input.getAvgPointA() > 100) {
                throw new IllegalArgumentException("Point A grade must be between 0 and 100, got: " + input.getAvgPointA());
            }
            if (input.getAvgPointB() < 0 || input.getAvgPointB() > 100) {
                throw new IllegalArgumentException("Point B grade must be between 0 and 100, got: " + input.getAvgPointB());
            }
            
            // Calculate team allowance
            double teamAllowance = (input.getAvgPointA() + input.getAvgPointB()) / 2.0 / 100.0;
            teamAllowance = roundToDecimalPrecision(teamAllowance);
            
            result.setFinalGrade(teamAllowance);
            result.getBreakdown().setTeamAllowance(teamAllowance);
            result.getBreakdown().addCalculationDetail(
                "team_allowance",
                String.format("AVG(Point A: %.2f, Point B: %.2f) / 100 = %.4f", 
                    input.getAvgPointA(), input.getAvgPointB(), teamAllowance)
            );
            
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage("Error calculating team allowance: " + e.getMessage());
            result.getBreakdown().addValidationError("team_allowance", e.getMessage());
        }
        
        return result;
    }

    /**
     * Calculates scaled grade for a deliverable.
     * Scaled Grade = Committee Grade × Deliverable Scalar
     * 
     * @param input Contains committee grade, scalar, weight, and deliverable name
     * @return GradingResult with scaled grade
     */
    public GradingResult calculateScaledDeliverableGrade(DeliverableGradeInput input) {
        GradingResult result = new GradingResult();
        
        try {
            // Defensive validation
            if (input == null) {
                throw new IllegalArgumentException("DeliverableGradeInput cannot be null");
            }
            if (input.getCommitteeGrade() == null) {
                throw new IllegalArgumentException("Committee grade cannot be null");
            }
            if (input.getScalar() == null) {
                throw new IllegalArgumentException("Scalar cannot be null");
            }
            if (input.getWeight() == null) {
                throw new IllegalArgumentException("Weight cannot be null");
            }
            
            // Validate values
            if (input.getCommitteeGrade() < 0 || input.getCommitteeGrade() > 100) {
                throw new IllegalArgumentException("Committee grade must be between 0 and 100, got: " + input.getCommitteeGrade());
            }
            if (input.getScalar() < 0 || input.getScalar() > 1.0) {
                throw new IllegalArgumentException("Scalar must be between 0 and 1.0, got: " + input.getScalar());
            }
            if (input.getWeight() < 0 || input.getWeight() > 100) {
                throw new IllegalArgumentException("Weight must be between 0 and 100, got: " + input.getWeight());
            }
            
            // Calculate scaled grade
            double scaledGrade = input.getCommitteeGrade() * input.getScalar();
            scaledGrade = roundToDecimalPrecision(scaledGrade);
            
            result.setFinalGrade(scaledGrade);
            result.getBreakdown().addScaledGrade(
                input.getDeliverableName() != null ? input.getDeliverableName() : "Deliverable",
                scaledGrade
            );
            result.getBreakdown().addCalculationDetail(
                input.getDeliverableName() != null ? input.getDeliverableName() : "Deliverable",
                String.format("Committee Grade: %.2f × Scalar: %.4f = Scaled Grade: %.2f", 
                    input.getCommitteeGrade(), input.getScalar(), scaledGrade)
            );
            
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage("Error calculating scaled deliverable grade: " + e.getMessage());
            result.getBreakdown().addValidationError("scaled_grade", e.getMessage());
        }
        
        return result;
    }

    /**
     * Calculates final team grade from weighted scaled deliverable grades.
     * Team Final Grade = SUM(Scaled Deliverable Grade × Weight / 100)
     * 
     * @param deliverableGrades List of scaled deliverable grades with weights
     * @return GradingResult with team final grade
     */
    public GradingResult calculateTeamFinalGrade(List<DeliverableGradeInput> deliverableGrades) {
        GradingResult result = new GradingResult();
        
        try {
            // Defensive validation
            if (deliverableGrades == null || deliverableGrades.isEmpty()) {
                throw new IllegalArgumentException("Deliverable grades list cannot be null or empty");
            }
            
            // Validate that weights sum to 100%
            double totalWeight = 0;
            for (DeliverableGradeInput grade : deliverableGrades) {
                if (grade.getWeight() == null) {
                    throw new IllegalArgumentException("Deliverable weight cannot be null in: " + grade.getDeliverableName());
                }
                totalWeight += grade.getWeight();
            }
            
            // Allow for floating point precision (99.9 to 100.1)
            if (totalWeight < 99.9 || totalWeight > 100.1) {
                throw new IllegalArgumentException(
                    String.format("Deliverable weights must sum to 100%%, got: %.2f%%", totalWeight)
                );
            }
            
            // Calculate team final grade
            double teamFinalGrade = 0;
            for (DeliverableGradeInput grade : deliverableGrades) {
                // First calculate scaled grade
                double scaledGrade = grade.getCommitteeGrade() * grade.getScalar();
                double weightedGrade = scaledGrade * (grade.getWeight() / 100.0);
                teamFinalGrade += weightedGrade;
                
                result.getBreakdown().addScaledGrade(grade.getDeliverableName(), scaledGrade);
                result.getBreakdown().addCalculationDetail(
                    grade.getDeliverableName(),
                    String.format("%.2f × %.4f × %.2f%% = %.2f", 
                        grade.getCommitteeGrade(), grade.getScalar(), grade.getWeight(), weightedGrade)
                );
            }
            
            teamFinalGrade = roundToDecimalPrecision(teamFinalGrade);
            
            result.setFinalGrade(teamFinalGrade);
            result.getBreakdown().setTeamFinalGrade(teamFinalGrade);
            
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage("Error calculating team final grade: " + e.getMessage());
            result.getBreakdown().addValidationError("team_final_grade", e.getMessage());
        }
        
        return result;
    }

    /**
     * Calculates individual student's final grade based on story point completion ratio.
     * Individual Grade = Team Final Grade × (Student Story Points / Total Story Points)
     * 
     * @param input Contains team final grade and story point information
     * @return GradingResult with individual final grade
     */
    public GradingResult calculateIndividualGrade(IndividualGradeInput input) {
        GradingResult result = new GradingResult();
        
        try {
            // Defensive validation
            if (input == null) {
                throw new IllegalArgumentException("IndividualGradeInput cannot be null");
            }
            if (input.getTeamFinalGrade() == null) {
                throw new IllegalArgumentException("Team final grade cannot be null");
            }
            if (input.getStudentStoryPointsCompleted() == null) {
                throw new IllegalArgumentException("Student story points completed cannot be null");
            }
            if (input.getTotalTeamStoryPointsTarget() == null) {
                throw new IllegalArgumentException("Total team story points target cannot be null");
            }
            
            // Validate values
            if (input.getTeamFinalGrade() < 0 || input.getTeamFinalGrade() > 100) {
                throw new IllegalArgumentException("Team final grade must be between 0 and 100, got: " + input.getTeamFinalGrade());
            }
            if (input.getStudentStoryPointsCompleted() < 0) {
                throw new IllegalArgumentException("Student story points cannot be negative, got: " + input.getStudentStoryPointsCompleted());
            }
            if (input.getTotalTeamStoryPointsTarget() <= 0) {
                throw new IllegalArgumentException("Total team story points must be positive, got: " + input.getTotalTeamStoryPointsTarget());
            }
            
            // Calculate story point ratio
            double storyPointRatio = input.getStudentStoryPointsCompleted() / input.getTotalTeamStoryPointsTarget();
            
            // Clamp ratio to [0, 1] range (student can't exceed total)
            if (storyPointRatio > 1.0) {
                storyPointRatio = 1.0;
            }
            
            // Calculate individual final grade
            double individualFinalGrade = input.getTeamFinalGrade() * storyPointRatio;
            individualFinalGrade = roundToDecimalPrecision(individualFinalGrade);
            
            result.setFinalGrade(individualFinalGrade);
            result.getBreakdown().setIndividualFinalGrade(individualFinalGrade);
            result.getBreakdown().setStudentStoryPointRatio(roundToDecimalPrecision(storyPointRatio));
            result.getBreakdown().addCalculationDetail(
                "individual_grade",
                String.format("Team Grade: %.2f × Story Point Ratio: %.4f (%.2f/%.2f) = Individual Grade: %.2f", 
                    input.getTeamFinalGrade(), storyPointRatio, 
                    input.getStudentStoryPointsCompleted(), input.getTotalTeamStoryPointsTarget(),
                    individualFinalGrade)
            );
            
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage("Error calculating individual grade: " + e.getMessage());
            result.getBreakdown().addValidationError("individual_grade", e.getMessage());
        }
        
        return result;
    }

    /**
     * Calculates the complete grading pipeline in one call.
     * This is a convenience method that orchestrates all grading steps.
     * 
     * @param deliverableGrades List of deliverables with committee grades, scalars, and weights
     * @param studentStoryPointsCompleted Story points completed by the student
     * @param totalTeamStoryPointsTarget Total story points target for the team
     * @return GradingResult with final individual grade and complete breakdown
     */
    public GradingResult calculateCompleteGrading(
            List<DeliverableGradeInput> deliverableGrades,
            Double studentStoryPointsCompleted,
            Double totalTeamStoryPointsTarget) {
        
        GradingResult result = new GradingResult();
        
        try {
            // Step 1: Calculate team final grade
            GradingResult teamGradeResult = calculateTeamFinalGrade(deliverableGrades);
            if (!teamGradeResult.getSuccess()) {
                return teamGradeResult;
            }
            
            // Step 2: Calculate individual grade
            IndividualGradeInput individualInput = new IndividualGradeInput(
                teamGradeResult.getFinalGrade(),
                studentStoryPointsCompleted,
                totalTeamStoryPointsTarget
            );
            GradingResult individualResult = calculateIndividualGrade(individualInput);
            if (!individualResult.getSuccess()) {
                return individualResult;
            }
            
            // Merge breakdowns
            GradingBreakdown finalBreakdown = teamGradeResult.getBreakdown();
            finalBreakdown.setIndividualFinalGrade(individualResult.getFinalGrade());
            finalBreakdown.setStudentStoryPointRatio(individualResult.getBreakdown().getStudentStoryPointRatio());
            finalBreakdown.getDeliverableCalculationDetails().putAll(
                individualResult.getBreakdown().getDeliverableCalculationDetails()
            );
            
            result.setFinalGrade(individualResult.getFinalGrade());
            result.setBreakdown(finalBreakdown);
            
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage("Error in complete grading calculation: " + e.getMessage());
        }
        
        return result;
    }

    // ============ Helper Methods ============

    /**
     * Validates that all grades are within valid range (0-100)
     */
    private void validateGradeValues(List<Double> grades, String gradeType) {
        for (Double grade : grades) {
            if (grade == null) {
                throw new IllegalArgumentException(gradeType + " grade cannot be null");
            }
            if (grade < 0 || grade > 100) {
                throw new IllegalArgumentException(
                    gradeType + " grade must be between 0 and 100, got: " + grade
                );
            }
        }
    }

    /**
     * Calculates average of a list of doubles
     */
    private double calculateAverage(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return 0;
        }
        double sum = 0;
        for (Double value : values) {
            sum += value;
        }
        return sum / values.size();
    }

    /**
     * Rounds a double to the specified decimal precision
     */
    private double roundToDecimalPrecision(double value) {
        double factor = Math.pow(10, DECIMAL_PRECISION);
        return Math.round(value * factor) / factor;
    }
}
