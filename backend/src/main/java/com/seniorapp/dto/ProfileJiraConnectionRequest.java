package com.seniorapp.dto;

import jakarta.validation.constraints.NotBlank;

public class ProfileJiraConnectionRequest {
    @NotBlank(message = "Jira site URL is required.")
    private String jiraSiteUrl;

    @NotBlank(message = "Jira API token is required.")
    private String jiraApiToken;

    public String getJiraSiteUrl() {
        return jiraSiteUrl;
    }

    public void setJiraSiteUrl(String jiraSiteUrl) {
        this.jiraSiteUrl = jiraSiteUrl;
    }

    public String getJiraApiToken() {
        return jiraApiToken;
    }

    public void setJiraApiToken(String jiraApiToken) {
        this.jiraApiToken = jiraApiToken;
    }
}
