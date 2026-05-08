package com.seniorapp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Sistemdeki tüm kullanıcıları temsil eden ana entity.
 * Hem GitHub üzerinden giriş yapan öğrencileri hem de
 * e-posta/şifre ile giriş yapan personel rollerini kapsar.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String email;

    private String password;

    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(unique = true)
    private String githubUsername;

    private Long githubId;

    @Column(name = "github_pat_encrypted", length = 1024)
    private String githubPatEncrypted;

    @Column(name = "jira_site_url_encrypted", columnDefinition = "TEXT")
    private String jiraSiteUrlEncrypted;

    @Column(name = "jira_api_token_encrypted", columnDefinition = "TEXT")
    private String jiraApiTokenEncrypted;

    @Column(name = "jira_refresh_token_encrypted", columnDefinition = "TEXT")
    private String jiraRefreshTokenEncrypted;

    @Column(name = "jira_account_id", length = 255)
    private String jiraAccountId;

    @Column(name = "jira_email", length = 255)
    private String jiraEmail;

    @Column(name = "jira_display_name", length = 255)
    private String jiraDisplayName;

    private boolean enabled = true;

    /**
     * Hesabın yaşam döngüsü durumu. GitHub ile oluşturulan hesaplarda
     * varsayılan olarak {@link UserStatus#ACTIVE} atanır.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public User() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getGithubUsername() { return githubUsername; }
    public void setGithubUsername(String githubUsername) { this.githubUsername = githubUsername; }

    public Long getGithubId() { return githubId; }
    public void setGithubId(Long githubId) { this.githubId = githubId; }

    public String getGithubPatEncrypted() { return githubPatEncrypted; }
    public void setGithubPatEncrypted(String githubPatEncrypted) { this.githubPatEncrypted = githubPatEncrypted; }

    public String getJiraSiteUrlEncrypted() { return jiraSiteUrlEncrypted; }
    public void setJiraSiteUrlEncrypted(String jiraSiteUrlEncrypted) { this.jiraSiteUrlEncrypted = jiraSiteUrlEncrypted; }

    public String getJiraApiTokenEncrypted() { return jiraApiTokenEncrypted; }
    public void setJiraApiTokenEncrypted(String jiraApiTokenEncrypted) { this.jiraApiTokenEncrypted = jiraApiTokenEncrypted; }

    public String getJiraRefreshTokenEncrypted() { return jiraRefreshTokenEncrypted; }
    public void setJiraRefreshTokenEncrypted(String jiraRefreshTokenEncrypted) { this.jiraRefreshTokenEncrypted = jiraRefreshTokenEncrypted; }

    public String getJiraAccountId() { return jiraAccountId; }
    public void setJiraAccountId(String jiraAccountId) { this.jiraAccountId = jiraAccountId; }

    public String getJiraEmail() { return jiraEmail; }
    public void setJiraEmail(String jiraEmail) { this.jiraEmail = jiraEmail; }

    public String getJiraDisplayName() { return jiraDisplayName; }
    public void setJiraDisplayName(String jiraDisplayName) { this.jiraDisplayName = jiraDisplayName; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }
}
