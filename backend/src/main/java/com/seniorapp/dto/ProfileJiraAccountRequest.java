package com.seniorapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ProfileJiraAccountRequest {
    @NotBlank(message = "Jira accountId is required.")
    @Size(max = 255, message = "Jira accountId is too long.")
    private String jiraAccountId;

    @Size(max = 255, message = "Jira email is too long.")
    private String jiraEmail;

    @Size(max = 255, message = "Jira display name is too long.")
    private String jiraDisplayName;

    public String getJiraAccountId() {
        return jiraAccountId;
    }

    public void setJiraAccountId(String jiraAccountId) {
        this.jiraAccountId = jiraAccountId;
    }

    public String getJiraEmail() {
        return jiraEmail;
    }

    public void setJiraEmail(String jiraEmail) {
        this.jiraEmail = jiraEmail;
    }

    public String getJiraDisplayName() {
        return jiraDisplayName;
    }

    public void setJiraDisplayName(String jiraDisplayName) {
        this.jiraDisplayName = jiraDisplayName;
    }
}
