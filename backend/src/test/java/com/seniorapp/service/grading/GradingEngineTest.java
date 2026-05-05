package com.seniorapp.service.grading;

import com.seniorapp.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for GradingEngine.
 * Tests cover:
 * - Happy path scenarios (normal cases)
 * - Edge cases (zeros, nulls, extreme values)
 * - Regression fixtures (predetermined results for consistency)
 * - Validation and error handling
 * - All critical calculation paths
 * 
 * These tests are designed to be CI-ready and deterministic.
 */
@DisplayName("GradingEngine Comprehensive Tests")
public class GradingEngineTest {

    private GradingEngine gradingEngine;

    @BeforeEach
    void setUp() {
        gradingEngine = new GradingEngine();
    }

    // ============ DELIVERABLE SCALAR TESTS ============

    @Test
    @DisplayName("Happy Path: Calculate deliverable scalar with normal grades")
    void testCalculateDeliverableScalar_HappyPath() {
        // Regression Fixture: A(100) and B(100) → scalar should be 1.0
        DeliverableScalarInput input = new DeliverableScalarInput(
            Arrays.asList(100.0),
            Arrays.asList(100.0),
            "Proposal"
        );

        GradingResult result = gradingEngine.calculateDeliverableScalar(input);

        assertTrue(result.getSuccess());
        assertEquals(1.0, result.getFinalGrade(), 0.01);
    }

    @Test
    @DisplayName("Regression: B(80) and A(100) grades → scalar 0.9")
    void testCalculateDeliverableScalar_BAndA_Grades() {
        // From Project Definition example: AVG(B(80), A(100)) = 90 → 0.9 scalar
        DeliverableScalarInput input = new DeliverableScalarInput(
            Arrays.asList(80.0),
            Arrays.asList(100.0),
            "Proposal"
        );

        GradingResult result = gradingEngine.calculateDeliverableScalar(input);

        assertTrue(result.getSuccess());
        assertEquals(0.9, result.getFinalGrade(), 0.01);
    }

    @Test
    @DisplayName("Regression: Multiple grades averaging to correct scalar")
    void testCalculateDeliverableScalar_MultipleGrades() {
        // From Project Definition: SoW with 3 sprints: AVG(B(80), A(100), A(100)) = 93.33 → 0.933
        DeliverableScalarInput input = new DeliverableScalarInput(
            Arrays.asList(80.0, 100.0, 100.0),
            Arrays.asList(80.0, 100.0, 100.0),
            "SoW"
        );

        GradingResult result = gradingEngine.calculateDeliverableScalar(input);

        assertTrue(result.getSuccess());
        // (80+100+100 + 80+100+100) / 2 / 100 = 186.67 / 100 = 0.933
        assertEquals(0.93, result.getFinalGrade(), 0.02);
    }

    @Test
    @DisplayName("Edge Case: Zero grades")
    void testCalculateDeliverableScalar_ZeroGrades() {
        DeliverableScalarInput input = new DeliverableScalarInput(
            Arrays.asList(0.0),
            Arrays.asList(0.0),
            "Failed Deliverable"
        );

        GradingResult result = gradingEngine.calculateDeliverableScalar(input);

        assertTrue(result.getSuccess());
        assertEquals(0.0, result.getFinalGrade(), 0.01);
    }

    @Test
    @DisplayName("Edge Case: Minimum passing grade (D=50)")
    void testCalculateDeliverableScalar_MinimumPassingGrade() {
        DeliverableScalarInput input = new DeliverableScalarInput(
            Arrays.asList(50.0),
            Arrays.asList(50.0),
            "Minimum"
        );

        GradingResult result = gradingEngine.calculateDeliverableScalar(input);

        assertTrue(result.getSuccess());
        assertEquals(0.5, result.getFinalGrade(), 0.01);
    }

    @Test
    @DisplayName("Validation Error: Null input")
    void testCalculateDeliverableScalar_NullInput() {
        GradingResult result = gradingEngine.calculateDeliverableScalar(null);

        assertFalse(result.getSuccess());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("null"));
    }

    @Test
    @DisplayName("Validation Error: Empty grade lists")
    void testCalculateDeliverableScalar_EmptyGradeLists() {
        DeliverableScalarInput input = new DeliverableScalarInput(
            Arrays.asList(),
            Arrays.asList(),
            "Empty"
        );

        GradingResult result = gradingEngine.calculateDeliverableScalar(input);

        assertFalse(result.getSuccess());
        assertTrue(result.getErrorMessage().contains("empty"));
    }

    @Test
    @DisplayName("Validation Error: Mismatched list sizes")
    void testCalculateDeliverableScalar_MismatchedListSizes() {
        DeliverableScalarInput input = new DeliverableScalarInput(
            Arrays.asList(100.0, 80.0),
            Arrays.asList(100.0),
            "Mismatch"
        );

        GradingResult result = gradingEngine.calculateDeliverableScalar(input);

        assertFalse(result.getSuccess());
        assertTrue(result.getErrorMessage().contains("same number"));
    }

    @Test
    @DisplayName("Validation Error: Invalid grade value (negative)")
    void testCalculateDeliverableScalar_NegativeGrade() {
        DeliverableScalarInput input = new DeliverableScalarInput(
            Arrays.asList(-10.0),
            Arrays.asList(100.0),
            "Negative"
        );

        GradingResult result = gradingEngine.calculateDeliverableScalar(input);

        assertFalse(result.getSuccess());
        assertTrue(result.getErrorMessage().contains("between 0 and 100"));
    }

    @Test
    @DisplayName("Validation Error: Invalid grade value (exceeds 100)")
    void testCalculateDeliverableScalar_GradeOver100() {
        DeliverableScalarInput input = new DeliverableScalarInput(
            Arrays.asList(150.0),
            Arrays.asList(100.0),
            "Over100"
        );

        GradingResult result = gradingEngine.calculateDeliverableScalar(input);

        assertFalse(result.getSuccess());
        assertTrue(result.getErrorMessage().contains("between 0 and 100"));
    }

    // ============ TEAM ALLOWANCE TESTS ============

    @Test
    @DisplayName("Happy Path: Calculate team allowance with normal grades")
    void testCalculateTeamAllowance_HappyPath() {
        // Regression: AVG(B(80), A(100)) = 90 → 0.9
        TeamAllowanceInput input = new TeamAllowanceInput(100.0, 80.0);

        GradingResult result = gradingEngine.calculateTeamAllowance(input);

        assertTrue(result.getSuccess());
        assertEquals(0.9, result.getFinalGrade(), 0.01);
    }

    @Test
    @DisplayName("Edge Case: Perfect scores")
    void testCalculateTeamAllowance_PerfectScores() {
        TeamAllowanceInput input = new TeamAllowanceInput(100.0, 100.0);

        GradingResult result = gradingEngine.calculateTeamAllowance(input);

        assertTrue(result.getSuccess());
        assertEquals(1.0, result.getFinalGrade(), 0.01);
    }

    @Test
    @DisplayName("Edge Case: Zero allowance")
    void testCalculateTeamAllowance_ZeroAllowance() {
        TeamAllowanceInput input = new TeamAllowanceInput(0.0, 0.0);

        GradingResult result = gradingEngine.calculateTeamAllowance(input);

        assertTrue(result.getSuccess());
        assertEquals(0.0, result.getFinalGrade(), 0.01);
    }

    @Test
    @DisplayName("Edge Case: Minimum passing allowance")
    void testCalculateTeamAllowance_MinimumPassing() {
        // D grade (50)
        TeamAllowanceInput input = new TeamAllowanceInput(50.0, 50.0);

        GradingResult result = gradingEngine.calculateTeamAllowance(input);

        assertTrue(result.getSuccess());
        assertEquals(0.5, result.getFinalGrade(), 0.01);
    }

    @Test
    @DisplayName("Validation Error: Null Point A")
    void testCalculateTeamAllowance_NullPointA() {
        TeamAllowanceInput input = new TeamAllowanceInput();
        input.setAvgPointA(null);
        input.setAvgPointB(100.0);

        GradingResult result = gradingEngine.calculateTeamAllowance(input);

        assertFalse(result.getSuccess());
        assertTrue(result.getErrorMessage().contains("null"));
    }

    @Test
    @DisplayName("Validation Error: Point A exceeds 100")
    void testCalculateTeamAllowance_PointAOver100() {
        TeamAllowanceInput input = new TeamAllowanceInput(150.0, 80.0);

        GradingResult result = gradingEngine.calculateTeamAllowance(input);

        assertFalse(result.getSuccess());
        assertTrue(result.getErrorMessage().contains("between 0 and 100"));
    }

    @Test
    @DisplayName("Validation Error: Negative Point B")
    void testCalculateTeamAllowance_NegativePointB() {
        TeamAllowanceInput input = new TeamAllowanceInput(100.0, -5.0);

        GradingResult result = gradingEngine.calculateTeamAllowance(input);

        assertFalse(result.getSuccess());
        assertTrue(result.getErrorMessage().contains("between 0 and 100"));
    }

    // ============ SCALED DELIVERABLE GRADE TESTS ============

    @Test
    @DisplayName("Happy Path: Scale deliverable grade")
    void testCalculateScaledDeliverableGrade_HappyPath() {
        // Regression: 90 (proposal grade) × 0.65 (proposal scalar) = 58.5
        DeliverableGradeInput input = new DeliverableGradeInput(
            "Proposal", 90.0, 0.65, 15.0
        );

        GradingResult result = gradingEngine.calculateScaledDeliverableGrade(input);

        assertTrue(result.getSuccess());
        assertEquals(58.5, result.getFinalGrade(), 0.01);
    }

    @Test
    @DisplayName("Regression: Multiple scaled grades from Project Definition")
    void testCalculateScaledDeliverableGrade_ProjectDefinitionExample() {
        // Proposal: 90 × 0.65 = 58.5
        DeliverableGradeInput input1 = new DeliverableGradeInput("Proposal", 90.0, 0.65, 15.0);
        GradingResult result1 = gradingEngine.calculateScaledDeliverableGrade(input1);
        assertEquals(58.5, result1.getFinalGrade(), 0.01);

        // SoW: 94 × 0.8 = 75.2
        DeliverableGradeInput input2 = new DeliverableGradeInput("SoW", 94.0, 0.8, 35.0);
        GradingResult result2 = gradingEngine.calculateScaledDeliverableGrade(input2);
        assertEquals(75.2, result2.getFinalGrade(), 0.01);

        // Demonstration: 92 × 0.865 = 79.58
        DeliverableGradeInput input3 = new DeliverableGradeInput("Demonstration", 92.0, 0.865, 50.0);
        GradingResult result3 = gradingEngine.calculateScaledDeliverableGrade(input3);
        assertEquals(79.58, result3.getFinalGrade(), 0.01);
    }

    @Test
    @DisplayName("Edge Case: Zero committee grade")
    void testCalculateScaledDeliverableGrade_ZeroCommitteeGrade() {
        DeliverableGradeInput input = new DeliverableGradeInput("Zero", 0.0, 0.75, 20.0);

        GradingResult result = gradingEngine.calculateScaledDeliverableGrade(input);

        assertTrue(result.getSuccess());
        assertEquals(0.0, result.getFinalGrade(), 0.01);
    }

    @Test
    @DisplayName("Edge Case: Zero scalar")
    void testCalculateScaledDeliverableGrade_ZeroScalar() {
        DeliverableGradeInput input = new DeliverableGradeInput("ZeroScalar", 100.0, 0.0, 50.0);

        GradingResult result = gradingEngine.calculateScaledDeliverableGrade(input);

        assertTrue(result.getSuccess());
        assertEquals(0.0, result.getFinalGrade(), 0.01);
    }

    @Test
    @DisplayName("Edge Case: Perfect grade and scalar")
    void testCalculateScaledDeliverableGrade_PerfectGradeAndScalar() {
        DeliverableGradeInput input = new DeliverableGradeInput("Perfect", 100.0, 1.0, 100.0);

        GradingResult result = gradingEngine.calculateScaledDeliverableGrade(input);

        assertTrue(result.getSuccess());
        assertEquals(100.0, result.getFinalGrade(), 0.01);
    }

    @Test
    @DisplayName("Validation Error: Null committee grade")
    void testCalculateScaledDeliverableGrade_NullCommitteeGrade() {
        DeliverableGradeInput input = new DeliverableGradeInput("Null", null, 0.75, 20.0);

        GradingResult result = gradingEngine.calculateScaledDeliverableGrade(input);

        assertFalse(result.getSuccess());
        assertTrue(result.getErrorMessage().contains("null"));
    }

    @Test
    @DisplayName("Validation Error: Invalid committee grade (exceeds 100)")
    void testCalculateScaledDeliverableGrade_InvalidCommitteeGrade() {
        DeliverableGradeInput input = new DeliverableGradeInput("Invalid", 150.0, 0.75, 20.0);

        GradingResult result = gradingEngine.calculateScaledDeliverableGrade(input);

        assertFalse(result.getSuccess());
        assertTrue(result.getErrorMessage().contains("between 0 and 100"));
    }

    @Test
    @DisplayName("Validation Error: Invalid scalar (exceeds 1.0)")
    void testCalculateScaledDeliverableGrade_InvalidScalar() {
        DeliverableGradeInput input = new DeliverableGradeInput("InvalidScalar", 90.0, 1.5, 20.0);

        GradingResult result = gradingEngine.calculateScaledDeliverableGrade(input);

        assertFalse(result.getSuccess());
        assertTrue(result.getErrorMessage().contains("between 0 and 1"));
    }

    // ============ TEAM FINAL GRADE TESTS ============

    @Test
    @DisplayName("Regression: Project Definition example - team final grade calculation")
    void testCalculateTeamFinalGrade_ProjectDefinitionExample() {
        // From Project Definition:
        // Proposal: 58.5 × 15% = 8.775
        // SoW: 75.2 × 35% = 26.32
        // Demo: 79.58 × 50% = 39.79
        // Total: 8.775 + 26.32 + 39.79 = 74.885
        
        List<DeliverableGradeInput> deliverables = Arrays.asList(
            new DeliverableGradeInput("Proposal", 90.0, 0.65, 15.0),
            new DeliverableGradeInput("SoW", 94.0, 0.8, 35.0),
            new DeliverableGradeInput("Demonstration", 92.0, 0.865, 50.0)
        );

        GradingResult result = gradingEngine.calculateTeamFinalGrade(deliverables);

        assertTrue(result.getSuccess());
        // Expected: 58.5*0.15 + 75.2*0.35 + 79.58*0.5 = 8.775 + 26.32 + 39.79 = 74.885
        assertEquals(74.88, result.getFinalGrade(), 0.1);
    }

    @Test
    @DisplayName("Happy Path: Two deliverables equal weight")
    void testCalculateTeamFinalGrade_TwoDeliverables() {
        List<DeliverableGradeInput> deliverables = Arrays.asList(
            new DeliverableGradeInput("Deliverable1", 100.0, 0.8, 50.0),
            new DeliverableGradeInput("Deliverable2", 80.0, 0.9, 50.0)
        );

        GradingResult result = gradingEngine.calculateTeamFinalGrade(deliverables);

        assertTrue(result.getSuccess());
        // (100*0.8*0.5) + (80*0.9*0.5) = 40 + 36 = 76
        assertEquals(76.0, result.getFinalGrade(), 0.01);
    }

    @Test
    @DisplayName("Edge Case: All zeros")
    void testCalculateTeamFinalGrade_AllZeros() {
        List<DeliverableGradeInput> deliverables = Arrays.asList(
            new DeliverableGradeInput("D1", 0.0, 0.0, 50.0),
            new DeliverableGradeInput("D2", 0.0, 0.0, 50.0)
        );

        GradingResult result = gradingEngine.calculateTeamFinalGrade(deliverables);

        assertTrue(result.getSuccess());
        assertEquals(0.0, result.getFinalGrade(), 0.01);
    }

    @Test
    @DisplayName("Edge Case: Perfect grades")
    void testCalculateTeamFinalGrade_PerfectGrades() {
        List<DeliverableGradeInput> deliverables = Arrays.asList(
            new DeliverableGradeInput("D1", 100.0, 1.0, 50.0),
            new DeliverableGradeInput("D2", 100.0, 1.0, 50.0)
        );

        GradingResult result = gradingEngine.calculateTeamFinalGrade(deliverables);

        assertTrue(result.getSuccess());
        assertEquals(100.0, result.getFinalGrade(), 0.01);
    }

    @Test
    @DisplayName("Validation Error: Weights don't sum to 100%")
    void testCalculateTeamFinalGrade_WeightsNotSumTo100() {
        List<DeliverableGradeInput> deliverables = Arrays.asList(
            new DeliverableGradeInput("D1", 100.0, 0.8, 30.0),
            new DeliverableGradeInput("D2", 100.0, 0.8, 30.0)
        );

        GradingResult result = gradingEngine.calculateTeamFinalGrade(deliverables);

        assertFalse(result.getSuccess());
        assertTrue(result.getErrorMessage().contains("100%"));
    }

    @Test
    @DisplayName("Validation Error: Empty deliverables list")
    void testCalculateTeamFinalGrade_EmptyDeliverables() {
        GradingResult result = gradingEngine.calculateTeamFinalGrade(Arrays.asList());

        assertFalse(result.getSuccess());
        assertTrue(result.getErrorMessage().contains("empty"));
    }

    @Test
    @DisplayName("Validation Error: Null deliverables list")
    void testCalculateTeamFinalGrade_NullDeliverables() {
        GradingResult result = gradingEngine.calculateTeamFinalGrade(null);

        assertFalse(result.getSuccess());
        assertTrue(result.getErrorMessage().contains("null"));
    }

    // ============ INDIVIDUAL GRADE TESTS ============

    @Test
    @DisplayName("Regression: Project Definition - individual grade calculation")
    void testCalculateIndividualGrade_ProjectDefinitionExample() {
        // From Project Example: Team Grade 74.88, Student completed 9/10 story points
        // Individual: 74.88 × (9/10) = 74.88 × 0.9 = 67.392
        IndividualGradeInput input = new IndividualGradeInput(74.88, 9.0, 10.0);

        GradingResult result = gradingEngine.calculateIndividualGrade(input);

        assertTrue(result.getSuccess());
        assertEquals(67.39, result.getFinalGrade(), 0.1);
    }

    @Test
    @DisplayName("Happy Path: Student completed all story points")
    void testCalculateIndividualGrade_StudentCompletedAll() {
        IndividualGradeInput input = new IndividualGradeInput(80.0, 10.0, 10.0);

        GradingResult result = gradingEngine.calculateIndividualGrade(input);

        assertTrue(result.getSuccess());
        assertEquals(80.0, result.getFinalGrade(), 0.01);
    }

    @Test
    @DisplayName("Edge Case: Student completed zero story points")
    void testCalculateIndividualGrade_ZeroStoryPoints() {
        IndividualGradeInput input = new IndividualGradeInput(80.0, 0.0, 10.0);

        GradingResult result = gradingEngine.calculateIndividualGrade(input);

        assertTrue(result.getSuccess());
        assertEquals(0.0, result.getFinalGrade(), 0.01);
    }

    @Test
    @DisplayName("Edge Case: Perfect team and individual performance")
    void testCalculateIndividualGrade_PerfectPerformance() {
        IndividualGradeInput input = new IndividualGradeInput(100.0, 10.0, 10.0);

        GradingResult result = gradingEngine.calculateIndividualGrade(input);

        assertTrue(result.getSuccess());
        assertEquals(100.0, result.getFinalGrade(), 0.01);
    }

    @Test
    @DisplayName("Edge Case: Zero team grade")
    void testCalculateIndividualGrade_ZeroTeamGrade() {
        IndividualGradeInput input = new IndividualGradeInput(0.0, 5.0, 10.0);

        GradingResult result = gradingEngine.calculateIndividualGrade(input);

        assertTrue(result.getSuccess());
        assertEquals(0.0, result.getFinalGrade(), 0.01);
    }

    @Test
    @DisplayName("Edge Case: Fractional story points")
    void testCalculateIndividualGrade_FractionalStoryPoints() {
        IndividualGradeInput input = new IndividualGradeInput(80.0, 3.5, 10.0);

        GradingResult result = gradingEngine.calculateIndividualGrade(input);

        assertTrue(result.getSuccess());
        assertEquals(28.0, result.getFinalGrade(), 0.01);
    }

    @Test
    @DisplayName("Validation Error: Null team grade")
    void testCalculateIndividualGrade_NullTeamGrade() {
        IndividualGradeInput input = new IndividualGradeInput(null, 5.0, 10.0);

        GradingResult result = gradingEngine.calculateIndividualGrade(input);

        assertFalse(result.getSuccess());
        assertTrue(result.getErrorMessage().contains("null"));
    }

    @Test
    @DisplayName("Validation Error: Negative student story points")
    void testCalculateIndividualGrade_NegativeStudentPoints() {
        IndividualGradeInput input = new IndividualGradeInput(80.0, -5.0, 10.0);

        GradingResult result = gradingEngine.calculateIndividualGrade(input);

        assertFalse(result.getSuccess());
        assertTrue(result.getErrorMessage().contains("negative"));
    }

    @Test
    @DisplayName("Validation Error: Zero total story points")
    void testCalculateIndividualGrade_ZeroTotalPoints() {
        IndividualGradeInput input = new IndividualGradeInput(80.0, 5.0, 0.0);

        GradingResult result = gradingEngine.calculateIndividualGrade(input);

        assertFalse(result.getSuccess());
        assertTrue(result.getErrorMessage().contains("positive"));
    }

    @Test
    @DisplayName("Edge Case: Student points exceed total (clamped to 1.0)")
    void testCalculateIndividualGrade_StudentPointsExceedTotal() {
        // Student completed 15 out of 10 target (150%)
        // Should be clamped to 100%
        IndividualGradeInput input = new IndividualGradeInput(80.0, 15.0, 10.0);

        GradingResult result = gradingEngine.calculateIndividualGrade(input);

        assertTrue(result.getSuccess());
        // Should get clamped to 80 × 1.0 = 80
        assertEquals(80.0, result.getFinalGrade(), 0.01);
    }

    // ============ COMPLETE GRADING PIPELINE TESTS ============

    @Test
    @DisplayName("Complete Pipeline: Full grading calculation")
    void testCalculateCompleteGrading_FullPipeline() {
        List<DeliverableGradeInput> deliverables = Arrays.asList(
            new DeliverableGradeInput("Proposal", 90.0, 0.65, 15.0),
            new DeliverableGradeInput("SoW", 94.0, 0.8, 35.0),
            new DeliverableGradeInput("Demonstration", 92.0, 0.865, 50.0)
        );

        GradingResult result = gradingEngine.calculateCompleteGrading(deliverables, 9.0, 10.0);

        assertTrue(result.getSuccess());
        assertNotNull(result.getFinalGrade());
        assertNotNull(result.getBreakdown());
        // Should be approximately 74.88 * 0.9 = 67.39
        assertEquals(67.39, result.getFinalGrade(), 0.1);
    }

    @Test
    @DisplayName("Complete Pipeline: Zero performance")
    void testCalculateCompleteGrading_ZeroPerformance() {
        List<DeliverableGradeInput> deliverables = Arrays.asList(
            new DeliverableGradeInput("D1", 0.0, 0.0, 50.0),
            new DeliverableGradeInput("D2", 0.0, 0.0, 50.0)
        );

        GradingResult result = gradingEngine.calculateCompleteGrading(deliverables, 0.0, 10.0);

        assertTrue(result.getSuccess());
        assertEquals(0.0, result.getFinalGrade(), 0.01);
    }

    @Test
    @DisplayName("Complete Pipeline: Perfect performance")
    void testCalculateCompleteGrading_PerfectPerformance() {
        List<DeliverableGradeInput> deliverables = Arrays.asList(
            new DeliverableGradeInput("D1", 100.0, 1.0, 50.0),
            new DeliverableGradeInput("D2", 100.0, 1.0, 50.0)
        );

        GradingResult result = gradingEngine.calculateCompleteGrading(deliverables, 10.0, 10.0);

        assertTrue(result.getSuccess());
        assertEquals(100.0, result.getFinalGrade(), 0.01);
    }

    @Test
    @DisplayName("Complete Pipeline: Breakdown contains calculation details")
    void testCalculateCompleteGrading_BreakdownDetails() {
        List<DeliverableGradeInput> deliverables = Arrays.asList(
            new DeliverableGradeInput("Proposal", 90.0, 0.65, 50.0),
            new DeliverableGradeInput("Demo", 80.0, 0.75, 50.0)
        );

        GradingResult result = gradingEngine.calculateCompleteGrading(deliverables, 5.0, 10.0);

        assertTrue(result.getSuccess());
        assertNotNull(result.getBreakdown().getTeamFinalGrade());
        assertNotNull(result.getBreakdown().getIndividualFinalGrade());
        assertFalse(result.getBreakdown().getDeliverableCalculationDetails().isEmpty());
        assertFalse(result.getBreakdown().getScaledDeliverableGrades().isEmpty());
    }

    @Test
    @DisplayName("Complete Pipeline: Invalid weight sum fails gracefully")
    void testCalculateCompleteGrading_InvalidWeightSum() {
        List<DeliverableGradeInput> deliverables = Arrays.asList(
            new DeliverableGradeInput("D1", 90.0, 0.8, 40.0),
            new DeliverableGradeInput("D2", 80.0, 0.8, 40.0)
        );

        GradingResult result = gradingEngine.calculateCompleteGrading(deliverables, 5.0, 10.0);

        assertFalse(result.getSuccess());
        assertNotNull(result.getErrorMessage());
    }
}
