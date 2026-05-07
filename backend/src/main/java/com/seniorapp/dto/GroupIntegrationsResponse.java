package com.seniorapp.dto;

public class GroupIntegrationsResponse {
    private String githubPat;
    private String jiraSpaceUrl;

    public GroupIntegrationsResponse(String githubPat, String jiraSpaceUrl) {
        this.githubPat = githubPat;
        this.jiraSpaceUrl = jiraSpaceUrl;
    }

    public String getGithubPat() {
        return githubPat;
    }

    public void setGithubPat(String githubPat) {
        this.githubPat = githubPat;
    }

    public String getJiraSpaceUrl() {
        return jiraSpaceUrl;
    }

    public void setJiraSpaceUrl(String jiraSpaceUrl) {
        this.jiraSpaceUrl = jiraSpaceUrl;
    }
}
