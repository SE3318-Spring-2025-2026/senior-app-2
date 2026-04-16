package com.seniorapp.entity;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "userGroups")
public class UserGroup {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "coordinator", referencedColumnName = "id")
  private User coordinator;

  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "teamLeader", referencedColumnName = "id")
  private User teamLeader;

  @OneToMany(cascade = CascadeType.ALL)
  private List<User> members;

  @Column(name = "github_pat_encrypted", length = 1024)
  private String githubPatEncrypted;

  @Column(name = "jira_space_url_encrypted", length = 1024)
  private String jiraSpaceUrlEncrypted;
}
