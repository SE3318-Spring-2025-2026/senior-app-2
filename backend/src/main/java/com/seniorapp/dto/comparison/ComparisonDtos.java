package com.seniorapp.dto.comparison;

import java.util.List;

public final class ComparisonDtos {
    private ComparisonDtos() {}

    // Main comparison response
    public static class ComparisonResponse {
        private JiraRequirement requirement;
        private String diff;
        private List<AIFeedbackItem> feedback;
        private List<HighlightedLine> highlightedLines;

        public ComparisonResponse() {}

        public JiraRequirement getRequirement() { return requirement; }
        public void setRequirement(JiraRequirement requirement) { this.requirement = requirement; }
        public String getDiff() { return diff; }
        public void setDiff(String diff) { this.diff = diff; }
        public List<AIFeedbackItem> getFeedback() { return feedback; }
        public void setFeedback(List<AIFeedbackItem> feedback) { this.feedback = feedback; }
        public List<HighlightedLine> getHighlightedLines() { return highlightedLines; }
        public void setHighlightedLines(List<HighlightedLine> highlightedLines) { this.highlightedLines = highlightedLines; }
    }

    // Jira requirement
    public static class JiraRequirement {
        private String id;
        private String key;
        private String summary;
        private String description;
        private Object acceptanceCriteria;
        private String priority;
        private String status;
        private String assignee;

        public JiraRequirement() {}

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Object getAcceptanceCriteria() { return acceptanceCriteria; }
        public void setAcceptanceCriteria(Object acceptanceCriteria) { this.acceptanceCriteria = acceptanceCriteria; }
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getAssignee() { return assignee; }
        public void setAssignee(String assignee) { this.assignee = assignee; }
    }

    // AI feedback item
    public static class AIFeedbackItem {
        private String id;
        private Integer lineNumber;
        private String severity;
        private String title;
        private String message;
        private String suggestion;
        private String status;

        public AIFeedbackItem() {}

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public Integer getLineNumber() { return lineNumber; }
        public void setLineNumber(Integer lineNumber) { this.lineNumber = lineNumber; }
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getSuggestion() { return suggestion; }
        public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    // Highlighted line
    public static class HighlightedLine {
        private Integer lineNumber;
        private String severity;
        private String message;

        public HighlightedLine() {}

        public HighlightedLine(Integer lineNumber, String severity, String message) {
            this.lineNumber = lineNumber;
            this.severity = severity;
            this.message = message;
        }

        public Integer getLineNumber() { return lineNumber; }
        public void setLineNumber(Integer lineNumber) { this.lineNumber = lineNumber; }
        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    // Feedback status update request
    public static class FeedbackStatusUpdate {
        private String status;

        public FeedbackStatusUpdate() {}

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
