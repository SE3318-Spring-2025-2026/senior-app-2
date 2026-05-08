package com.seniorapp.service.jira;

import com.seniorapp.dto.jira.JiraDtos;
import com.seniorapp.entity.Project;
import com.seniorapp.entity.ProjectStudentStoryPoint;
import com.seniorapp.entity.UserGroup;
import com.seniorapp.entity.UserGroupMember;
import com.seniorapp.entity.GroupInviteStatus;
import com.seniorapp.entity.GroupMembershipRole;
import com.seniorapp.repository.ProjectGroupAssignmentRepository;
import com.seniorapp.repository.ProjectRepository;
import com.seniorapp.repository.ProjectStudentStoryPointRepository;
import com.seniorapp.repository.UserGroupMemberRepository;
import com.seniorapp.repository.UserGroupRepository;
import com.seniorapp.service.GitHubPrMatcherService;
import com.seniorapp.service.IntegrationCredentialCryptoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Daily Sync Scheduler.
 *
 * <p>Runs at 01:00 every night and:
 * <ol>
 *   <li>Iterates over all active project-group assignments.</li>
 *   <li>For each group that has Jira credentials configured, fetches open-sprint issues.</li>
 *   <li>Matches Jira issues to student members by assignee email.</li>
 *   <li>Accumulates the story points and upserts {@link ProjectStudentStoryPoint} records.</li>
 * </ol>
 *
 * <p>If the Jira token is missing or invalid the group is skipped and an error is logged
 * (the scheduler never throws so other groups are unaffected).
 *
 * <p>DO NOT modify {@code GradingEngine.java} – it consumes {@link ProjectStudentStoryPoint}
 * automatically.
 */
@Component
public class DailySyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(DailySyncScheduler.class);

    /** System user id used when upserting story points via the scheduler. */
    private static final long SYSTEM_USER_ID = -1L;

    private final JiraIntegrationService jiraIntegrationService;
    private final UserGroupRepository userGroupRepository;
    private final UserGroupMemberRepository userGroupMemberRepository;
    private final ProjectGroupAssignmentRepository projectGroupAssignmentRepository;
    private final ProjectRepository projectRepository;
    private final ProjectStudentStoryPointRepository storyPointRepository;
    private final GitHubPrMatcherService githubPrMatcherService;
    private final IntegrationCredentialCryptoService cryptoService;

    public DailySyncScheduler(JiraIntegrationService jiraIntegrationService,
                               UserGroupRepository userGroupRepository,
                               UserGroupMemberRepository userGroupMemberRepository,
                               ProjectGroupAssignmentRepository projectGroupAssignmentRepository,
                               ProjectRepository projectRepository,
                               ProjectStudentStoryPointRepository storyPointRepository,
                               GitHubPrMatcherService githubPrMatcherService,
                               IntegrationCredentialCryptoService cryptoService) {
        this.jiraIntegrationService = jiraIntegrationService;
        this.userGroupRepository = userGroupRepository;
        this.userGroupMemberRepository = userGroupMemberRepository;
        this.projectGroupAssignmentRepository = projectGroupAssignmentRepository;
        this.projectRepository = projectRepository;
        this.storyPointRepository = storyPointRepository;
        this.githubPrMatcherService = githubPrMatcherService;
        this.cryptoService = cryptoService;
    }

    /**
     * Nightly sync job: 01:00 AM every day.
     * Package-private so tests can invoke it directly.
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void syncJiraStoryPoints() {
        log.info("[DailySyncScheduler] Starting nightly Jira ↔ story-point sync...");
        int groupsProcessed = 0;
        int groupsSkipped = 0;

        List<UserGroup> allGroups = userGroupRepository.findAll();
        for (UserGroup group : allGroups) {
            try {
                boolean synced = syncGroup(group);
                if (synced) groupsProcessed++;
                else groupsSkipped++;
            } catch (Exception ex) {
                log.error("[DailySyncScheduler] Failed to sync group id={} name='{}': {}",
                        group.getId(), group.getGroupName(), ex.getMessage());
                groupsSkipped++;
            }
        }

        log.info("[DailySyncScheduler] Sync complete. Processed={} Skipped={}", groupsProcessed, groupsSkipped);
    }

    // ── Per-group sync ─────────────────────────────────────────────────────────

    /**
     * Syncs story points for a single group.
     *
     * @return {@code true} if the sync was performed, {@code false} if skipped (no Jira config)
     */
    boolean syncGroup(UserGroup group) {
        String encryptedJiraToken = group.getJiraSpaceUrlEncrypted();
        if (encryptedJiraToken == null || encryptedJiraToken.isBlank()) {
            log.debug("[DailySyncScheduler] Group id={} has no Jira token – skipping.", group.getId());
            return false;
        }

        // Find associated project
        var assignmentOpt = projectGroupAssignmentRepository.findByGroupIdAndActiveTrue(group.getId());
        if (assignmentOpt.isEmpty()) {
            log.debug("[DailySyncScheduler] Group id={} has no active project assignment – skipping.", group.getId());
            return false;
        }

        Project project = projectRepository.findById(assignmentOpt.get().getProject().getId()).orElse(null);
        if (project == null) {
            return false;
        }

        // Extract Jira domain and project key from the stored URL
        String jiraDomain = extractJiraDomain(encryptedJiraToken); // token IS the domain in this context
        // The jiraSpaceUrlEncrypted stores the full Jira URL (e.g. https://acme.atlassian.net)
        // We store the encrypted form; but for domain extraction we need the decrypted URL.
        // Here we delegate to JiraIntegrationService which uses SecureOutboundApiService internally.
        // For the scheduler, the domain comes from the group's stored (plaintext) Jira space URL after decrypt.
        // See UserGroup.jiraSpaceUrlEncrypted – the entity listener stores the decrypted URL in this field at runtime.

        // Use the project term as the Jira project key (convention in this system)
        String projectKey = project.getTerm() != null ? project.getTerm() : project.getTitle();
        String jql = jiraIntegrationService.buildOpenSprintJql(projectKey);

        List<JiraDtos.Issue> issues = jiraIntegrationService.fetchIssuesByJql(
                jiraDomain, encryptedJiraToken, jql);

        // Accepted student members of this group
        List<UserGroupMember> studentMembers = userGroupMemberRepository
                .findByGroupIdAndStatus(group.getId(), GroupInviteStatus.ACCEPTED)
                .stream()
                .filter(m -> m.getRole() == GroupMembershipRole.MEMBER || m.getRole() == GroupMembershipRole.LEADER)
                .toList();

        upsertStoryPoints(project, group, studentMembers, issues);
        return true;
    }

    /**
     * For each student member, sums the story points of all Jira issues assigned to them
     * that meet the "Done" criteria AND have a merged GitHub PR.
     */
    void upsertStoryPoints(Project project, UserGroup group, List<UserGroupMember> members, List<JiraDtos.Issue> issues) {
        String decryptedPat = null;
        if (group.getGithubPatEncrypted() != null) {
            decryptedPat = cryptoService.decrypt(group.getGithubPatEncrypted());
        }

        for (UserGroupMember member : members) {
            String memberEmail = member.getUser().getEmail();
            if (memberEmail == null) continue;

            final String finalPat = decryptedPat;

            double totalPoints = issues.stream()
                    .filter(issue -> {
                        JiraDtos.IssueFields fields = issue.getFields();
                        if (fields == null) return false;
                        
                        // 1. Assignee matching
                        JiraDtos.Assignee assignee = fields.getAssignee();
                        if (assignee == null || !memberEmail.equalsIgnoreCase(assignee.getEmailAddress())) {
                            return false;
                        }

                        // 2. Status check (Optional but recommended: only count "Done" tasks)
                        // Note: Some Jira boards use different status names, so we check "Done" case-insensitive
                        if (fields.getStatus() != null && fields.getStatus().getName() != null) {
                            String status = fields.getStatus().getName().toLowerCase();
                            if (!status.equals("done") && !status.equals("resolved") && !status.equals("completed")) {
                                return false;
                            }
                        }

                        // 3. GitHub PR Merge Verification (CRITICAL GAP FILL)
                        if (finalPat != null && group.getGithubRepoOwner() != null && group.getGithubRepoName() != null) {
                            boolean hasMergedPr = githubPrMatcherService.findFirstMerged(
                                    group.getGithubRepoOwner(),
                                    group.getGithubRepoName(),
                                    issue.getKey(),
                                    finalPat
                            ).isPresent();
                            
                            if (!hasMergedPr) {
                                log.debug("[DailySyncScheduler] Issue {} is Done in Jira but has no merged PR in {}/{} - skipping points.",
                                        issue.getKey(), group.getGithubRepoOwner(), group.getGithubRepoName());
                                return false;
                            }
                        }

                        return true;
                    })
                    .mapToDouble(issue -> {
                        Double sp = issue.getFields().getStoryPoints();
                        return sp != null ? sp : 0.0;
                    })
                    .sum();

            if (totalPoints == 0.0) {
                // No assigned issues found – do not overwrite existing data
                log.debug("[DailySyncScheduler] No Jira story points found for user {} in project {}",
                        memberEmail, project.getId());
                continue;
            }

            Long studentUserId = member.getUser().getId();
            ProjectStudentStoryPoint ssp = storyPointRepository
                    .findByProject_IdAndStudentUserId(project.getId(), studentUserId)
                    .orElseGet(() -> {
                        ProjectStudentStoryPoint newSsp = new ProjectStudentStoryPoint();
                        newSsp.setProject(project);
                        newSsp.setStudentUserId(studentUserId);
                        return newSsp;
                    });

            ssp.setStoryPoints(totalPoints);
            ssp.setUpdatedByUserId(SYSTEM_USER_ID);
            storyPointRepository.save(ssp);

            log.info("[DailySyncScheduler] Upserted story points {} for userId={} in projectId={}",
                    totalPoints, studentUserId, project.getId());
        }
    }

    /** Extracts host name from a Jira space URL string. Falls back to the raw value. */
    String extractJiraDomain(String jiraSpaceUrl) {
        if (jiraSpaceUrl == null) return "";
        try {
            java.net.URI uri = new java.net.URI(jiraSpaceUrl);
            String host = uri.getHost();
            return host != null ? host : jiraSpaceUrl;
        } catch (Exception e) {
            return jiraSpaceUrl;
        }
    }
}
