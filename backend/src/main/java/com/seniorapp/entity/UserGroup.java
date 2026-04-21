package com.seniorapp.entity;

import java.util.List;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "user_groups")
public class UserGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String groupName;

    @OneToOne
    @JoinColumn(name = "coordinator_id", referencedColumnName = "id")
    private User coordinator;

    @OneToOne
    @JoinColumn(name = "team_leader_id", referencedColumnName = "id")
    private User teamLeader;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserGroupMember> memberships;

    @Column(name = "github_pat_encrypted", length = 1024)
    private String githubPatEncrypted;

    @Column(name = "jira_space_url_encrypted", length = 1024)
    private String jiraSpaceUrlEncrypted;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public User getCoordinator() {
        return coordinator;
    }

    public void setCoordinator(User coordinator) {
        this.coordinator = coordinator;
    }

    public User getTeamLeader() {
        return teamLeader;
    }

    public void setTeamLeader(User teamLeader) {
        this.teamLeader = teamLeader;
    }

    public List<UserGroupMember> getMemberships() {
        return memberships;
    }

    public void setMemberships(List<UserGroupMember> memberships) {
        this.memberships = memberships;
    }

    public String getGithubPatEncrypted() {
        return githubPatEncrypted;
    }

    public void setGithubPatEncrypted(String githubPatEncrypted) {
        this.githubPatEncrypted = githubPatEncrypted;
    }

    public String getJiraSpaceUrlEncrypted() {
        return jiraSpaceUrlEncrypted;
    }

    public void setJiraSpaceUrlEncrypted(String jiraSpaceUrlEncrypted) {
        this.jiraSpaceUrlEncrypted = jiraSpaceUrlEncrypted;
    }
}