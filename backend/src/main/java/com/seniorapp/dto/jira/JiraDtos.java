package com.seniorapp.dto.jira;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Jira REST API response DTOs.
 * All fields are lenient ({@code @JsonIgnoreProperties}) so that unknown Jira fields are silently dropped.
 */
public final class JiraDtos {

    private JiraDtos() {}

    // ── /rest/agile/1.0/board/{boardId}/sprint?state=active ──────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SprintSearchResponse {
        private List<Sprint> values;

        public List<Sprint> getValues() { return values; }
        public void setValues(List<Sprint> values) { this.values = values; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Sprint {
        private Long id;
        private String name;
        private String state;  // "active", "closed", "future"
        private String goal;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getState() { return state; }
        public void setState(String state) { this.state = state; }
        public String getGoal() { return goal; }
        public void setGoal(String goal) { this.goal = goal; }
    }

    // ── /rest/api/3/search?jql=... ───────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IssueSearchResponse {
        private int total;
        private List<Issue> issues;

        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }
        public List<Issue> getIssues() { return issues; }
        public void setIssues(List<Issue> issues) { this.issues = issues; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Issue {
        private String id;
        private String key;
        private IssueFields fields;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public IssueFields getFields() { return fields; }
        public void setFields(IssueFields fields) { this.fields = fields; }
    }

    /**
     * Jira issue fields.
     * {@code storyPoints} is mapped from a Jira custom field.
     * The actual field key (e.g. "story_points" or "customfield_10016") is obtained via
     * {@link com.seniorapp.service.jira.JiraIntegrationService#STORY_POINTS_FIELD}.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IssueFields {
        private String summary;
        private String description;
        private Status status;
        private Priority priority;
        private Assignee assignee;

        /**
         * Story points value – populated dynamically by the service
         * after extracting the custom field by name from the raw JSON node.
         */
        private Double storyPoints;

        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Status getStatus() { return status; }
        public void setStatus(Status status) { this.status = status; }
        public Priority getPriority() { return priority; }
        public void setPriority(Priority priority) { this.priority = priority; }
        public Assignee getAssignee() { return assignee; }
        public void setAssignee(Assignee assignee) { this.assignee = assignee; }
        public Double getStoryPoints() { return storyPoints; }
        public void setStoryPoints(Double storyPoints) { this.storyPoints = storyPoints; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Status {
        private String name;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Priority {
        private String name;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Assignee {
        @JsonProperty("displayName")
        private String displayName;

        @JsonProperty("emailAddress")
        private String emailAddress;

        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public String getEmailAddress() { return emailAddress; }
        public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }
    }
}
