package com.seniorapp.dto.ai;

/**
 * Enhanced DTOs for the Level 4 AI Implementation Validator
 * (Jira description ↔ GitHub PR file diff comparison).
 */
public final class AlignmentDtos {

    private AlignmentDtos() {}

    /**
     * Structured result from the enhanced
     * {@link com.seniorapp.service.ai.TaskCodeAlignmentValidatorService#validateAlignment} method.
     *
     * <p>Alignment score measures whether the code change actually addresses the Jira task.
     * Completeness score measures whether ALL acceptance criteria are covered.
     */
    public static class AlignmentResult {

        /** 0.0 – 1.0: how closely the PR diff matches the Jira task description and acceptance criteria. */
        private float alignmentScore;

        /**
         * 0.0 – 1.0: how completely the PR addresses all acceptance criteria.
         * A "fake" PR (unrelated code) should score near 0.0.
         * A partial implementation should score between 0.3 and 0.7.
         */
        private float completenessScore;

        /** Human-readable explanation of the scores. */
        private String reasoning;

        /** Short summary (1-2 sentences). */
        private String summary;

        /** Was a timeout thrown and caught? Only set when a fallback was used. */
        private boolean timedOut;

        public AlignmentResult() {}

        public AlignmentResult(float alignmentScore, float completenessScore,
                               String reasoning, String summary) {
            this.alignmentScore = alignmentScore;
            this.completenessScore = completenessScore;
            this.reasoning = reasoning;
            this.summary = summary;
        }

        public float getAlignmentScore() { return alignmentScore; }
        public void setAlignmentScore(float alignmentScore) { this.alignmentScore = alignmentScore; }
        public float getCompletenessScore() { return completenessScore; }
        public void setCompletenessScore(float completenessScore) { this.completenessScore = completenessScore; }
        public String getReasoning() { return reasoning; }
        public void setReasoning(String reasoning) { this.reasoning = reasoning; }
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        public boolean isTimedOut() { return timedOut; }
        public void setTimedOut(boolean timedOut) { this.timedOut = timedOut; }

        @Override
        public String toString() {
            return "AlignmentResult{alignment=" + alignmentScore + ", completeness=" + completenessScore + "}";
        }
    }
}
