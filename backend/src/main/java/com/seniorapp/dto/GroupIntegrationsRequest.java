package com.seniorapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class GroupIntegrationsRequest {

    @NotBlank(message = "GitHub token is required.")
    @Pattern(
            regexp = "^(ghp_[A-Za-z0-9_]{20,255}|github_pat_[A-Za-z0-9_]{20,255})$",
            message = "Invalid GitHub token format."
    )
    private String githubPat;

    @NotBlank(message = "JIRA workspace URL is required.")
    @Pattern(
            regexp = "^https?://.+$",
            message = "JIRA workspace URL must start with http:// or https://."
    )
    private String jiraSpaceUrl;

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
