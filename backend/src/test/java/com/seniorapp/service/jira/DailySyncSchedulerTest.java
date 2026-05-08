package com.seniorapp.service.jira;

import com.seniorapp.dto.jira.JiraDtos;
import com.seniorapp.entity.*;
import com.seniorapp.repository.*;
import com.seniorapp.service.GitHubPrMatcherService;
import com.seniorapp.service.IntegrationCredentialCryptoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DailySyncScheduler – Aggressive Unit Tests")
class DailySyncSchedulerTest {

    @Mock private JiraIntegrationService jiraIntegrationService;
    @Mock private UserGroupRepository userGroupRepository;
    @Mock private UserGroupMemberRepository userGroupMemberRepository;
    @Mock private ProjectGroupAssignmentRepository projectGroupAssignmentRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectStudentStoryPointRepository storyPointRepository;
    @Mock private GitHubPrMatcherService githubPrMatcherService;
    @Mock private IntegrationCredentialCryptoService cryptoService;

    @InjectMocks
    private DailySyncScheduler scheduler;

    private User studentUser;
    private UserGroup group;
    private Project project;
    private ProjectGroupAssignment assignment;
    private UserGroupMember studentMember;

    @BeforeEach
    void setUp() {
        studentUser = new User();
        studentUser.setId(10L);
        studentUser.setEmail("alice@test.com");
        studentUser.setRole(Role.STUDENT);

        group = new UserGroup();
        group.setId(1L);
        group.setGroupName("Team Alpha");
        group.setJiraSpaceUrlEncrypted("https://acme.atlassian.net");

        project = new Project();
        project.setId(100L);
        project.setTitle("MYAPP");
        project.setTerm("MYAPP");

        assignment = new ProjectGroupAssignment();
        assignment.setProject(project);
        assignment.setGroupId(group.getId());
        assignment.setActive(true);

        studentMember = new UserGroupMember();
        studentMember.setUser(studentUser);
        studentMember.setStatus(GroupInviteStatus.ACCEPTED);
        studentMember.setRole(GroupMembershipRole.MEMBER);
    }

    @Nested
    @DisplayName("syncJiraStoryPoints() – full scheduler run")
    class FullSchedulerTests {

        @Test
        @DisplayName("Happy path: story points are upserted for assigned student with merged PR")
        void happy_path_upserts_story_points() {
            when(userGroupRepository.findAll()).thenReturn(List.of(group));
            when(projectGroupAssignmentRepository.findByGroupIdAndActiveTrue(1L))
                    .thenReturn(Optional.of(assignment));
            when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
            when(jiraIntegrationService.buildOpenSprintJql("MYAPP")).thenReturn("project=\"MYAPP\" AND sprint in openSprints()");
            when(jiraIntegrationService.fetchIssuesByJql(anyString(), anyString(), anyString()))
                    .thenReturn(List.of(makeIssue("alice@test.com", 8.0, "Done")));
            
            group.setGithubRepoOwner("owner");
            group.setGithubRepoName("repo");
            group.setGithubPatEncrypted("enc-pat");
            when(cryptoService.decrypt("enc-pat")).thenReturn("dec-pat");
            when(githubPrMatcherService.findFirstMerged("owner", "repo", "PROJ-1", "dec-pat"))
                    .thenReturn(Optional.of(new GitHubPrMatcherService.PrMatch(1, "t", "b", "closed", true, "url")));

            when(userGroupMemberRepository.findByGroupIdAndStatus(1L, GroupInviteStatus.ACCEPTED))
                    .thenReturn(List.of(studentMember));
            when(storyPointRepository.findByProject_IdAndStudentUserId(100L, 10L))
                    .thenReturn(Optional.empty());
            when(storyPointRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            scheduler.syncJiraStoryPoints();

            verify(storyPointRepository).save(argThat(ssp ->
                    ssp.getStoryPoints() == 8.0 && ssp.getStudentUserId().equals(10L)));
        }

        @Test
        @DisplayName("Issue is Done but has no merged PR – skipped")
        void done_issue_no_merged_pr_skipped() {
            when(userGroupRepository.findAll()).thenReturn(List.of(group));
            when(projectGroupAssignmentRepository.findByGroupIdAndActiveTrue(1L))
                    .thenReturn(Optional.of(assignment));
            when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
            when(jiraIntegrationService.buildOpenSprintJql(anyString())).thenReturn("jql");
            when(jiraIntegrationService.fetchIssuesByJql(anyString(), anyString(), anyString()))
                    .thenReturn(List.of(makeIssue("alice@test.com", 5.0, "Done")));
            
            group.setGithubRepoOwner("owner");
            group.setGithubRepoName("repo");
            group.setGithubPatEncrypted("pat");
            when(cryptoService.decrypt("pat")).thenReturn("dec-pat");
            when(githubPrMatcherService.findFirstMerged(anyString(), anyString(), anyString(), anyString()))
                    .thenReturn(Optional.empty());

            when(userGroupMemberRepository.findByGroupIdAndStatus(1L, GroupInviteStatus.ACCEPTED))
                    .thenReturn(List.of(studentMember));

            scheduler.syncJiraStoryPoints();

            verify(storyPointRepository, never()).save(any());
        }

        @Test
        @DisplayName("Group without Jira token – skipped")
        void group_without_jira_token_skipped() {
            group.setJiraSpaceUrlEncrypted(null);
            when(userGroupRepository.findAll()).thenReturn(List.of(group));
            scheduler.syncJiraStoryPoints();
            verifyNoInteractions(jiraIntegrationService);
        }
    }

    @Nested
    @DisplayName("upsertStoryPoints() – unit tests")
    class UpsertStoryPointsTests {

        @Test
        @DisplayName("Missing story points – skipped")
        void missing_story_points_skipped() {
            scheduler.upsertStoryPoints(project, group, List.of(studentMember), 
                    List.of(makeIssue("alice@test.com", null, "Done")));
            verifyNoInteractions(storyPointRepository);
        }

        @Test
        @DisplayName("Student has no email – skipped")
        void student_no_email_skipped() {
            studentUser.setEmail(null);
            scheduler.upsertStoryPoints(project, group, List.of(studentMember), 
                    List.of(makeIssue("alice@test.com", 5.0, "Done")));
            verifyNoInteractions(storyPointRepository);
        }
    }

    @Nested
    @DisplayName("extractJiraDomain() – domain extraction")
    class ExtractJiraDomainTests {
        @Test
        void extracts_host() {
            assertThat(scheduler.extractJiraDomain("https://acme.atlassian.net")).isEqualTo("acme.atlassian.net");
        }
    }

    private JiraDtos.Issue makeIssue(String assigneeEmail, Double storyPoints, String statusName) {
        JiraDtos.Issue issue = new JiraDtos.Issue();
        issue.setId("1");
        issue.setKey("PROJ-1");

        JiraDtos.IssueFields fields = new JiraDtos.IssueFields();
        fields.setSummary("Test");
        fields.setStoryPoints(storyPoints);

        if (statusName != null) {
            JiraDtos.Status status = new JiraDtos.Status();
            status.setName(statusName);
            fields.setStatus(status);
        }

        if (assigneeEmail != null) {
            JiraDtos.Assignee assignee = new JiraDtos.Assignee();
            assignee.setEmailAddress(assigneeEmail);
            fields.setAssignee(assignee);
        }

        issue.setFields(fields);
        return issue;
    }
}
