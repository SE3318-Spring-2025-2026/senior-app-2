package com.seniorapp.dto;

public class ProfileGithubPatStatusResponse {
    private boolean configured;

    public ProfileGithubPatStatusResponse(boolean configured) {
        this.configured = configured;
    }

    public boolean isConfigured() {
        return configured;
    }
}
