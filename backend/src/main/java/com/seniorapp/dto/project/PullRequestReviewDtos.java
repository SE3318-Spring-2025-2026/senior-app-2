package com.seniorapp.dto.project;

import com.seniorapp.dto.comparison.ComparisonDtos;

import java.util.List;

public final class PullRequestReviewDtos {
    private PullRequestReviewDtos() {}

    public static class PullRequestReviewResponse {
        private Long projectId;
        private Integer prNumber;
        private String prTitle;
        private String prUrl;
        private String baseBranch;
        private String headBranch;
        private String diff;
        private IssueInfo issue;
        private AiWidget reviewProcessAi;
        private AiWidget implementationAi;

        public Long getProjectId() { return projectId; }
        public void setProjectId(Long projectId) { this.projectId = projectId; }
        public Integer getPrNumber() { return prNumber; }
        public void setPrNumber(Integer prNumber) { this.prNumber = prNumber; }
        public String getPrTitle() { return prTitle; }
        public void setPrTitle(String prTitle) { this.prTitle = prTitle; }
        public String getPrUrl() { return prUrl; }
        public void setPrUrl(String prUrl) { this.prUrl = prUrl; }
        public String getBaseBranch() { return baseBranch; }
        public void setBaseBranch(String baseBranch) { this.baseBranch = baseBranch; }
        public String getHeadBranch() { return headBranch; }
        public void setHeadBranch(String headBranch) { this.headBranch = headBranch; }
        public String getDiff() { return diff; }
        public void setDiff(String diff) { this.diff = diff; }
        public IssueInfo getIssue() { return issue; }
        public void setIssue(IssueInfo issue) { this.issue = issue; }
        public AiWidget getReviewProcessAi() { return reviewProcessAi; }
        public void setReviewProcessAi(AiWidget reviewProcessAi) { this.reviewProcessAi = reviewProcessAi; }
        public AiWidget getImplementationAi() { return implementationAi; }
        public void setImplementationAi(AiWidget implementationAi) { this.implementationAi = implementationAi; }
    }

    public static class IssueInfo {
        private String key;
        private String title;
        private String description;
        private String assignee;
        private Double storyPoints;

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getAssignee() { return assignee; }
        public void setAssignee(String assignee) { this.assignee = assignee; }
        public Double getStoryPoints() { return storyPoints; }
        public void setStoryPoints(Double storyPoints) { this.storyPoints = storyPoints; }
    }

    public static class AiWidget {
        private String title;
        private Float accuracyScore;
        private String summary;
        private List<String> discrepancies;
        private List<String> evidence;

        public static AiWidget from(String title, ComparisonDtos.AIFeedbackItemList src) {
            AiWidget dto = new AiWidget();
            dto.setTitle(title);
            if (src != null) {
                dto.setAccuracyScore(src.getAccuracyScore());
                dto.setSummary(src.getSummary());
                dto.setDiscrepancies(src.getDiscrepancies());
                dto.setEvidence(src.getEvidence());
            }
            return dto;
        }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public Float getAccuracyScore() { return accuracyScore; }
        public void setAccuracyScore(Float accuracyScore) { this.accuracyScore = accuracyScore; }
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        public List<String> getDiscrepancies() { return discrepancies; }
        public void setDiscrepancies(List<String> discrepancies) { this.discrepancies = discrepancies; }
        public List<String> getEvidence() { return evidence; }
        public void setEvidence(List<String> evidence) { this.evidence = evidence; }
    }
}
