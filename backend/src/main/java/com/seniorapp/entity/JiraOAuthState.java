package com.seniorapp.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "jira_oauth_states")
public class JiraOAuthState {
    @Id
    private String state;
    private Long userId;
    private LocalDateTime createdAt;

    public JiraOAuthState() {
    }

    public JiraOAuthState(String state, Long userId, LocalDateTime createdAt) {
        this.state = state;
        this.userId = userId;
        this.createdAt = createdAt;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
