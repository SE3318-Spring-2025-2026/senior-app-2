package com.seniorapp.service.jira;

import com.seniorapp.dto.jira.JiraDtos;
import com.seniorapp.entity.*;
import com.seniorapp.repository.*;
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

/**
 * Aggressive unit tests for {@link DailySyncScheduler}.
 *
 * Edge cases covered:
 * - Cron execution happy path (story points upserted)
 * - Groups with no Jira token → skipped gracefully
 * - Groups with no active project assignment → skipped gracefully
 * - Jira API throws 429 rate limit → group skipped, scheduler continues
 * - Story points = null (missing custom field) → not saved, logged only
 * - Student with no email → skipped
 * - extractJiraDomain with various URL formats
 * - Story point calculation: sum of multiple issues per student
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DailySyncScheduler – Aggressive Unit Tests")
class DailySyncSchedulerTest {

    @Mock private JiraIntegrationService jiraIntegrationService;
    @Mock private UserGroupRepository userGroupRepository;
    @Mock private UserGroupMemberRepository userGroupMemberRepository;
    @Mock private ProjectGroupAssignmentRepository projectGroupAssignmentRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectStudentStoryPointRepository storyPointRepository;

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

    // ══════════════════════════════════════════════════════════════════════════
    // syncJiraStoryPoints() – full scheduler invocation
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("syncJiraStoryPoints() – full scheduler run")
    class FullSchedulerTests {

        @Test
        @DisplayName("Happy path: story points are upserted for assigned student")
        void happy_path_upserts_story_points() {
            when(userGroupRepository.findAll()).thenReturn(List.of(group));
            when(projectGroupAssignmentRepository.findByGroupIdAndActiveTrue(1L))
                    .thenReturn(Optional.of(assignment));
            when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
            when(jiraIntegrationService.buildOpenSprintJql("MYAPP")).thenReturn("project=\"MYAPP\" AND sprint in openSprints()");
            when(jiraIntegrationService.fetchIssuesByJql(anyString(), anyString(), anyString()))
                    .thenReturn(List.of(makeIssue("alice@test.com", 8.0)));
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
        @DisplayName("Multiple issues assigned to same student – story points are summed")
        void multiple_issues_story_points_are_summed() {
            when(userGroupRepository.findAll()).thenReturn(List.of(group));
            when(projectGroupAssignmentRepository.findByGroupIdAndActiveTrue(1L))
                    .thenReturn(Optional.of(assignment));
            when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
            when(jiraIntegrationService.buildOpenSprintJql(anyString())).thenReturn("jql");
            when(jiraIntegrationService.fetchIssuesByJql(anyString(), anyString(), anyString()))
                    .thenReturn(List.of(
                            makeIssue("alice@test.com", 5.0),
                            makeIssue("alice@test.com", 3.0),
                            makeIssue("bob@test.com", 8.0)   // different user
                    ));
            when(userGroupMemberRepository.findByGroupIdAndStatus(1L, GroupInviteStatus.ACCEPTED))
                    .thenReturn(List.of(studentMember));
            when(storyPointRepository.findByProject_IdAndStudentUserId(100L, 10L))
                    .thenReturn(Optional.empty());
            when(storyPointRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            scheduler.syncJiraStoryPoints();

            verify(storyPointRepository).save(argThat(ssp -> ssp.getStoryPoints() == 8.0)); // 5+3
        }

        @Test
        @DisplayName("Jira API throws rate limit 429 for one group – scheduler continues with other groups")
        void rate_limit_skips_group_but_continues() {
            UserGroup group2 = new UserGroup();
            group2.setId(2L);
            group2.setGroupName("Team Beta");
            group2.setJiraSpaceUrlEncrypted("https://beta.atlassian.net");

            Project project2 = new Project();
            project2.setId(200L);
            project2.setTitle("BETA");
            project2.setTerm("BETA");

            ProjectGroupAssignment assignment2 = new ProjectGroupAssignment();
            assignment2.setProject(project2);
            assignment2.setGroupId(2L);
            assignment2.setActive(true);

            when(userGroupRepository.findAll()).thenReturn(List.of(group, group2));

            // group 1: returns assignment but Jira API rate-limits
            when(projectGroupAssignmentRepository.findByGroupIdAndActiveTrue(1L))
                    .thenReturn(Optional.of(assignment));
            when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
            when(jiraIntegrationService.buildOpenSprintJql("MYAPP")).thenReturn("jql1");
            when(jiraIntegrationService.fetchIssuesByJql(contains("acme"), anyString(), anyString()))
                    .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS));

            // group 2: succeeds
            when(projectGroupAssignmentRepository.findByGroupIdAndActiveTrue(2L))
                    .thenReturn(Optional.of(assignment2));
            when(projectRepository.findById(200L)).thenReturn(Optional.of(project2));
            when(jiraIntegrationService.buildOpenSprintJql("BETA")).thenReturn("jql2");
            when(jiraIntegrationService.fetchIssuesByJql(contains("beta"), anyString(), anyString()))
                    .thenReturn(List.of(makeIssue("alice@test.com", 3.0)));
            when(userGroupMemberRepository.findByGroupIdAndStatus(2L, GroupInviteStatus.ACCEPTED))
                    .thenReturn(List.of(studentMember));
            when(storyPointRepository.findByProject_IdAndStudentUserId(200L, 10L)).thenReturn(Optional.empty());
            when(storyPointRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Should NOT throw; group 1 error is swallowed, group 2 is processed
            assertDoesNotThrow(() -> scheduler.syncJiraStoryPoints());

            verify(storyPointRepository, times(1)).save(any());
        }

        @Test
        @DisplayName("Group without Jira token – skipped, no Jira call made")
        void group_without_jira_token_skipped() {
            UserGroup noTokenGroup = new UserGroup();
            noTokenGroup.setId(99L);
            noTokenGroup.setGroupName("No Token Group");
            noTokenGroup.setJiraSpaceUrlEncrypted(null);

            when(userGroupRepository.findAll()).thenReturn(List.of(noTokenGroup));

            scheduler.syncJiraStoryPoints();

            verifyNoInteractions(jiraIntegrationService);
            verifyNoInteractions(storyPointRepository);
        }

        @Test
        @DisplayName("Group without active project assignment – skipped")
        void group_without_project_assignment_skipped() {
            when(userGroupRepository.findAll()).thenReturn(List.of(group));
            when(projectGroupAssignmentRepository.findByGroupIdAndActiveTrue(1L)).thenReturn(Optional.empty());

            scheduler.syncJiraStoryPoints();

            verifyNoInteractions(jiraIntegrationService);
            verifyNoInteractions(storyPointRepository);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // upsertStoryPoints() – unit tests
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("upsertStoryPoints() – unit tests")
    class UpsertStoryPointsTests {

        @Test
        @DisplayName("Missing story points (null) – existing record not overwritten")
        void missing_story_points_does_not_overwrite_existing() {
            // Issue has no story points
            JiraDtos.Issue issueWithNoSp = makeIssue("alice@test.com", null);

            scheduler.upsertStoryPoints(project, List.of(studentMember), List.of(issueWithNoSp));

            verifyNoInteractions(storyPointRepository);
        }

        @Test
        @DisplayName("Student has no email – issue matching is skipped gracefully")
        void student_without_email_skipped() {
            studentUser.setEmail(null);
            JiraDtos.Issue issue = makeIssue("alice@test.com", 5.0);

            scheduler.upsertStoryPoints(project, List.of(studentMember), List.of(issue));

            verifyNoInteractions(storyPointRepository);
        }

        @Test
        @DisplayName("Existing story point record is updated in-place")
        void existing_record_is_updated() {
            ProjectStudentStoryPoint existing = new ProjectStudentStoryPoint();
            existing.setProject(project);
            existing.setStudentUserId(10L);
            existing.setStoryPoints(3.0);

            when(storyPointRepository.findByProject_IdAndStudentUserId(100L, 10L))
                    .thenReturn(Optional.of(existing));
            when(storyPointRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            scheduler.upsertStoryPoints(project, List.of(studentMember),
                    List.of(makeIssue("alice@test.com", 13.0)));

            verify(storyPointRepository).save(argThat(ssp -> ssp.getStoryPoints() == 13.0));
        }

        @Test
        @DisplayName("Email comparison is case-insensitive")
        void email_comparison_is_case_insensitive() {
            studentUser.setEmail("Alice@Test.COM");
            when(storyPointRepository.findByProject_IdAndStudentUserId(100L, 10L)).thenReturn(Optional.empty());
            when(storyPointRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            scheduler.upsertStoryPoints(project, List.of(studentMember),
                    List.of(makeIssue("alice@test.com", 6.0)));

            verify(storyPointRepository).save(any());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // extractJiraDomain() – unit tests
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("extractJiraDomain() – domain extraction")
    class ExtractJiraDomainTests {

        @Test
        @DisplayName("Extracts host from full https URL")
        void extracts_host_from_https_url() {
            assertThat(scheduler.extractJiraDomain("https://acme.atlassian.net")).isEqualTo("acme.atlassian.net");
        }

        @Test
        @DisplayName("Returns raw value when not a valid URI")
        void returns_raw_value_for_invalid_uri() {
            assertThat(scheduler.extractJiraDomain("enc:v1:abc:def")).isEqualTo("enc:v1:abc:def");
        }

        @Test
        @DisplayName("Returns empty string for null")
        void returns_empty_for_null() {
            assertThat(scheduler.extractJiraDomain(null)).isEmpty();
        }
    }

    // ── Helper ─────────────────────────────────────────────────────────────────

    private JiraDtos.Issue makeIssue(String assigneeEmail, Double storyPoints) {
        JiraDtos.Issue issue = new JiraDtos.Issue();
        issue.setId("1");
        issue.setKey("PROJ-1");

        JiraDtos.IssueFields fields = new JiraDtos.IssueFields();
        fields.setSummary("Do something");
        fields.setStoryPoints(storyPoints);

        if (assigneeEmail != null) {
            JiraDtos.Assignee assignee = new JiraDtos.Assignee();
            assignee.setEmailAddress(assigneeEmail);
            fields.setAssignee(assignee);
        }

        issue.setFields(fields);
        return issue;
    }
}
