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

    // CascadeType.ALL kaldırıldı: Grup silindiğinde kullanıcıların silinmemesi için.
    // @JoinColumn: Arada gereksiz bir "user_groups_members" tablosu oluşmasını engeller.
    @OneToMany
    @JoinColumn(name = "group_id")
    private List<User> members;

    @Column(name = "github_pat_encrypted", length = 1024)
    private String githubPatEncrypted;

    @Column(name = "jira_space_url_encrypted", length = 1024)
    private String jiraSpaceUrlEncrypted;
}