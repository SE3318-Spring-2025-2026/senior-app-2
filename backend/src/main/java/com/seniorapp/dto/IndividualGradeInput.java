package com.seniorapp.dto;

/**
 * Represents input data for calculating an individual student's final grade.
 * Individual grade is the team's final grade multiplied by the student's story point ratio.
 */
public class IndividualGradeInput {
    
    /**
     * Final team grade (after applying all deliverable weights and scalars)
     */
    private Double teamFinalGrade;
    
    /**
     * Story points completed by this student
     */
    private Double studentStoryPointsCompleted;
    
    /**
     * Total team story points target for the evaluation period
     */
    private Double totalTeamStoryPointsTarget;
    
    /**
     * Student identifier if needed for logging/debugging
     */
    private Long studentId;

    public IndividualGradeInput() {}

    public IndividualGradeInput(Double teamFinalGrade, Double studentStoryPointsCompleted, 
                                Double totalTeamStoryPointsTarget) {
        this.teamFinalGrade = teamFinalGrade;
        this.studentStoryPointsCompleted = studentStoryPointsCompleted;
        this.totalTeamStoryPointsTarget = totalTeamStoryPointsTarget;
    }

    public IndividualGradeInput(Double teamFinalGrade, Double studentStoryPointsCompleted, 
                                Double totalTeamStoryPointsTarget, Long studentId) {
        this.teamFinalGrade = teamFinalGrade;
        this.studentStoryPointsCompleted = studentStoryPointsCompleted;
        this.totalTeamStoryPointsTarget = totalTeamStoryPointsTarget;
        this.studentId = studentId;
    }

    public Double getTeamFinalGrade() {
        return teamFinalGrade;
    }

    public void setTeamFinalGrade(Double teamFinalGrade) {
        this.teamFinalGrade = teamFinalGrade;
    }

    public Double getStudentStoryPointsCompleted() {
        return studentStoryPointsCompleted;
    }

    public void setStudentStoryPointsCompleted(Double studentStoryPointsCompleted) {
        this.studentStoryPointsCompleted = studentStoryPointsCompleted;
    }

    public Double getTotalTeamStoryPointsTarget() {
        return totalTeamStoryPointsTarget;
    }

    public void setTotalTeamStoryPointsTarget(Double totalTeamStoryPointsTarget) {
        this.totalTeamStoryPointsTarget = totalTeamStoryPointsTarget;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }
}
