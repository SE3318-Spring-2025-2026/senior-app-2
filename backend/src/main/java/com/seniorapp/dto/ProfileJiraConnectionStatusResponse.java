package com.seniorapp.dto;

public class ProfileJiraConnectionStatusResponse {
    private boolean configured;
    private String jiraSiteUrl;

    public ProfileJiraConnectionStatusResponse(boolean configured, String jiraSiteUrl) {
        this.configured = configured;
        this.jiraSiteUrl = jiraSiteUrl;
    }

    public boolean isConfigured() {
        return configured;
    }

    public String getJiraSiteUrl() {
        return jiraSiteUrl;
    }
}
