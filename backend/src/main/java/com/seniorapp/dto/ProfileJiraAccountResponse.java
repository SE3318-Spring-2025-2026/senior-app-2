package com.seniorapp.dto;

public class ProfileJiraAccountResponse {
    private String jiraAccountId;
    private String jiraEmail;
    private String jiraDisplayName;

    public ProfileJiraAccountResponse(String jiraAccountId, String jiraEmail, String jiraDisplayName) {
        this.jiraAccountId = jiraAccountId;
        this.jiraEmail = jiraEmail;
        this.jiraDisplayName = jiraDisplayName;
    }

    public String getJiraAccountId() {
        return jiraAccountId;
    }

    public String getJiraEmail() {
        return jiraEmail;
    }

    public String getJiraDisplayName() {
        return jiraDisplayName;
    }
}
