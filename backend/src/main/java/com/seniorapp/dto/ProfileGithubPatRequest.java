package com.seniorapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ProfileGithubPatRequest {
    @NotBlank(message = "GitHub token is required.")
    @Pattern(
            regexp = "^(ghp_[A-Za-z0-9_]{20,255}|github_pat_[A-Za-z0-9_]{20,255})$",
            message = "Invalid GitHub token format.")
    private String githubPat;

    public String getGithubPat() {
        return githubPat;
    }

    public void setGithubPat(String githubPat) {
        this.githubPat = githubPat;
    }
}
