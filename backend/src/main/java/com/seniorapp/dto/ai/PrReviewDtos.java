package com.seniorapp.dto.ai;

/**
 * DTOs for the AI Pull-Request Review Validator.
 */
public final class PrReviewDtos {

    private PrReviewDtos() {}

    /**
     * Structured result returned by {@link com.seniorapp.service.ai.AiPullRequestReviewValidatorService}.
     */
    public static class PrReviewResult {

        /** 0–100 meaningfulness score. */
        private int reviewScore;

        /** True if the review contains meaningful software-engineering discussion. */
        private boolean meaningful;

        /** One-paragraph human-readable summary of the AI's assessment. */
        private String summary;

        /** Raw feedback / reasoning from the AI (optional, for audit). */
        private String reasoning;

        public PrReviewResult() {}

        public PrReviewResult(int reviewScore, boolean meaningful, String summary, String reasoning) {
            this.reviewScore = reviewScore;
            this.meaningful = meaningful;
            this.summary = summary;
            this.reasoning = reasoning;
        }

        public int getReviewScore() { return reviewScore; }
        public void setReviewScore(int reviewScore) { this.reviewScore = reviewScore; }
        public boolean isMeaningful() { return meaningful; }
        public void setMeaningful(boolean meaningful) { this.meaningful = meaningful; }
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        public String getReasoning() { return reasoning; }
        public void setReasoning(String reasoning) { this.reasoning = reasoning; }

        @Override
        public String toString() {
            return "PrReviewResult{score=" + reviewScore + ", meaningful=" + meaningful + "}";
        }
    }

    /**
     * Input to the validator: metadata about a pull request together with its review comments.
     */
    public static class PrReviewInput {
        private String repoOwner;
        private String repoName;
        private int prNumber;
        private String prTitle;
        private String prBody;

        public PrReviewInput() {}

        public String getRepoOwner() { return repoOwner; }
        public void setRepoOwner(String repoOwner) { this.repoOwner = repoOwner; }
        public String getRepoName() { return repoName; }
        public void setRepoName(String repoName) { this.repoName = repoName; }
        public int getPrNumber() { return prNumber; }
        public void setPrNumber(int prNumber) { this.prNumber = prNumber; }
        public String getPrTitle() { return prTitle; }
        public void setPrTitle(String prTitle) { this.prTitle = prTitle; }
        public String getPrBody() { return prBody; }
        public void setPrBody(String prBody) { this.prBody = prBody; }
    }
}
