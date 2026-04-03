package com.seniorapp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GitHubUserResponse {
    private Long id;
    private String login; // GitHub kullanıcı adı
    private String email;

    public Long getId() { return id; }
    public String getLogin() { return login; }
    public String getEmail() { return email; }
}