package com.seniorapp.dto.project;

import java.util.ArrayList;
import java.util.List;

/**
 * PDF &quot;Project Definition V2&quot; grading outputs attached to {@link ProjectDtos.ProjectDetail}
 * for the active project group. Numbers are team-level unless {@link #individualAllowanceFactor} differs from 1.
 */
public class ProjectGradingSummaryDto {

    /** Fixed label for clients; algorithm details live in {@link #modelDescription}. */
    private String engineVersion = "pdf-v2-integrated";

    /**
     * Human-readable summary of how numbers were derived (simplified vs full PDF matrix / story points).
     */
    private String modelDescription;

    /** Weighted sum of scaled deliverable grades (0–100 scale), PDF &quot;Documents + Demonstration&quot; style total. */
    private Double cumulativeTeamGrade;

    /** Unweighted arithmetic mean of {@link DeliverableGradingLineDto#getRawSuccessGrade()} where present. */
    private Double overallSuccessGrade;

    /**
     * Legacy single scalar; kept for API compatibility. When per-student manual story points exist, see
     * {@link #studentStoryPointLines} and {@link #manualStoryPointsTeamSum} — this field stays {@code 1.0}.
     */
    private Double individualAllowanceFactor = 1.0;

    /** {@code cumulativeTeamGrade * individualAllowanceFactor} */
    private Double adjustedIndividualGrade;

    /** Weighted mean of per-deliverable sprint scalars (same weights as deliverables). */
    private Double weightedMeanSprintScalar;

    /** Sum of {@link StudentStoryPointGradingLineDto#getManualStoryPoints()} for students in the active group (nulls skipped). */
    private Double manualStoryPointsTeamSum;

    /** Accepted students in the group (for interpreting story point coverage). */
    private Integer manualStoryPointsStudentCount;

    /** How many of those students have a non-null manual story point saved. */
    private Integer manualStoryPointsEnteredCount;

    private List<SprintProcessLineDto> sprintProcessLines = new ArrayList<>();
    private List<DeliverableGradingLineDto> deliverableLines = new ArrayList<>();

    /**
     * Per-student manual story points (advisor UI) and suggested grade {@code cumulativeTeamGrade * (sp / teamSum)}
     * when team sum &gt; 0.
     */
    private List<StudentStoryPointGradingLineDto> studentStoryPointLines = new ArrayList<>();

    public String getEngineVersion() {
        return engineVersion;
    }

    public void setEngineVersion(String engineVersion) {
        this.engineVersion = engineVersion;
    }

    public String getModelDescription() {
        return modelDescription;
    }

    public void setModelDescription(String modelDescription) {
        this.modelDescription = modelDescription;
    }

    public Double getCumulativeTeamGrade() {
        return cumulativeTeamGrade;
    }

    public void setCumulativeTeamGrade(Double cumulativeTeamGrade) {
        this.cumulativeTeamGrade = cumulativeTeamGrade;
    }

    public Double getOverallSuccessGrade() {
        return overallSuccessGrade;
    }

    public void setOverallSuccessGrade(Double overallSuccessGrade) {
        this.overallSuccessGrade = overallSuccessGrade;
    }

    public Double getIndividualAllowanceFactor() {
        return individualAllowanceFactor;
    }

    public void setIndividualAllowanceFactor(Double individualAllowanceFactor) {
        this.individualAllowanceFactor = individualAllowanceFactor;
    }

    public Double getAdjustedIndividualGrade() {
        return adjustedIndividualGrade;
    }

    public void setAdjustedIndividualGrade(Double adjustedIndividualGrade) {
        this.adjustedIndividualGrade = adjustedIndividualGrade;
    }

    public Double getWeightedMeanSprintScalar() {
        return weightedMeanSprintScalar;
    }

    public void setWeightedMeanSprintScalar(Double weightedMeanSprintScalar) {
        this.weightedMeanSprintScalar = weightedMeanSprintScalar;
    }

    public Double getManualStoryPointsTeamSum() {
        return manualStoryPointsTeamSum;
    }

    public void setManualStoryPointsTeamSum(Double manualStoryPointsTeamSum) {
        this.manualStoryPointsTeamSum = manualStoryPointsTeamSum;
    }

    public Integer getManualStoryPointsStudentCount() {
        return manualStoryPointsStudentCount;
    }

    public void setManualStoryPointsStudentCount(Integer manualStoryPointsStudentCount) {
        this.manualStoryPointsStudentCount = manualStoryPointsStudentCount;
    }

    public Integer getManualStoryPointsEnteredCount() {
        return manualStoryPointsEnteredCount;
    }

    public void setManualStoryPointsEnteredCount(Integer manualStoryPointsEnteredCount) {
        this.manualStoryPointsEnteredCount = manualStoryPointsEnteredCount;
    }

    public List<StudentStoryPointGradingLineDto> getStudentStoryPointLines() {
        return studentStoryPointLines;
    }

    public void setStudentStoryPointLines(List<StudentStoryPointGradingLineDto> studentStoryPointLines) {
        this.studentStoryPointLines =
                studentStoryPointLines != null ? studentStoryPointLines : new ArrayList<>();
    }

    public List<SprintProcessLineDto> getSprintProcessLines() {
        return sprintProcessLines;
    }

    public void setSprintProcessLines(List<SprintProcessLineDto> sprintProcessLines) {
        this.sprintProcessLines = sprintProcessLines != null ? sprintProcessLines : new ArrayList<>();
    }

    public List<DeliverableGradingLineDto> getDeliverableLines() {
        return deliverableLines;
    }

    public void setDeliverableLines(List<DeliverableGradingLineDto> deliverableLines) {
        this.deliverableLines = deliverableLines != null ? deliverableLines : new ArrayList<>();
    }

    public static class StudentStoryPointGradingLineDto {
        private Long studentUserId;
        private String fullName;
        private Double manualStoryPoints;
        /** {@code manualStoryPoints / manualStoryPointsTeamSum} when sum &gt; 0 and this student has a value. */
        private Double storyPointShareOfTeam;
        /** {@code cumulativeTeamGrade * storyPointShareOfTeam} when both defined. */
        private Double suggestedIndividualGrade;

        public Long getStudentUserId() {
            return studentUserId;
        }

        public void setStudentUserId(Long studentUserId) {
            this.studentUserId = studentUserId;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public Double getManualStoryPoints() {
            return manualStoryPoints;
        }

        public void setManualStoryPoints(Double manualStoryPoints) {
            this.manualStoryPoints = manualStoryPoints;
        }

        public Double getStoryPointShareOfTeam() {
            return storyPointShareOfTeam;
        }

        public void setStoryPointShareOfTeam(Double storyPointShareOfTeam) {
            this.storyPointShareOfTeam = storyPointShareOfTeam;
        }

        public Double getSuggestedIndividualGrade() {
            return suggestedIndividualGrade;
        }

        public void setSuggestedIndividualGrade(Double suggestedIndividualGrade) {
            this.suggestedIndividualGrade = suggestedIndividualGrade;
        }
    }

    public static class SprintProcessLineDto {
        private Integer sprintNo;
        private Double scrumEvaluationAvg;
        private Double reviewEvaluationAvg;
        /** Average of scrum/review branch scores before dividing by 100 (0–100). */
        private Double processMidScore;
        /** Capped {@code processMidScore / 100} used as sprint scalar. */
        private Double sprintScalar;

        public Integer getSprintNo() {
            return sprintNo;
        }

        public void setSprintNo(Integer sprintNo) {
            this.sprintNo = sprintNo;
        }

        public Double getScrumEvaluationAvg() {
            return scrumEvaluationAvg;
        }

        public void setScrumEvaluationAvg(Double scrumEvaluationAvg) {
            this.scrumEvaluationAvg = scrumEvaluationAvg;
        }

        public Double getReviewEvaluationAvg() {
            return reviewEvaluationAvg;
        }

        public void setReviewEvaluationAvg(Double reviewEvaluationAvg) {
            this.reviewEvaluationAvg = reviewEvaluationAvg;
        }

        public Double getProcessMidScore() {
            return processMidScore;
        }

        public void setProcessMidScore(Double processMidScore) {
            this.processMidScore = processMidScore;
        }

        public Double getSprintScalar() {
            return sprintScalar;
        }

        public void setSprintScalar(Double sprintScalar) {
            this.sprintScalar = sprintScalar;
        }
    }

    public static class DeliverableGradingLineDto {
        private Long deliverableId;
        private String title;
        private Integer weight;
        /** Committee deliverable rubric average (all graders, all rubrics) for the group submission. */
        private Double rawSuccessGrade;
        private Double sprintProcessMidScore;
        private Double sprintScalar;
        private Double scaledGrade;

        public Long getDeliverableId() {
            return deliverableId;
        }

        public void setDeliverableId(Long deliverableId) {
            this.deliverableId = deliverableId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public Integer getWeight() {
            return weight;
        }

        public void setWeight(Integer weight) {
            this.weight = weight;
        }

        public Double getRawSuccessGrade() {
            return rawSuccessGrade;
        }

        public void setRawSuccessGrade(Double rawSuccessGrade) {
            this.rawSuccessGrade = rawSuccessGrade;
        }

        public Double getSprintProcessMidScore() {
            return sprintProcessMidScore;
        }

        public void setSprintProcessMidScore(Double sprintProcessMidScore) {
            this.sprintProcessMidScore = sprintProcessMidScore;
        }

        public Double getSprintScalar() {
            return sprintScalar;
        }

        public void setSprintScalar(Double sprintScalar) {
            this.sprintScalar = sprintScalar;
        }

        public Double getScaledGrade() {
            return scaledGrade;
        }

        public void setScaledGrade(Double scaledGrade) {
            this.scaledGrade = scaledGrade;
        }
    }
}
