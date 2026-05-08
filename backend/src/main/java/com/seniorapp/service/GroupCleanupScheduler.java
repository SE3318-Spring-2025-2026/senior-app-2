package com.seniorapp.service;

import com.seniorapp.entity.GroupInviteStatus;
import com.seniorapp.entity.ProjectTemplate;
import com.seniorapp.entity.Role;
import com.seniorapp.entity.UserGroup;
import com.seniorapp.entity.UserGroupMember;
import com.seniorapp.repository.ProjectTemplateRepository;
import com.seniorapp.repository.UserGroupMemberRepository;
import com.seniorapp.repository.UserGroupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Nightly background job that automatically disbands groups without an advisor
 * after the group formation deadline has passed.
 *
 * <p>Schedule: every day at midnight (00:00:00 server time).
 *
 * <p>Logic:
 * <ol>
 *   <li>Fetch all active {@link ProjectTemplate}s.</li>
 *   <li>If no active template has a past {@code groupFormationDeadline}, do nothing.</li>
 *   <li>Otherwise fetch all {@link UserGroup}s.</li>
 *   <li>For each group, check whether it has at least one ACCEPTED member whose
 *       role is {@code PROFESSOR} or {@code COORDINATOR} – that member is the advisor.</li>
 *   <li>Groups without an advisor are deleted via
 *       {@link GroupService#systemDeleteGroup(Long)}.</li>
 * </ol>
 */
@Component
public class GroupCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(GroupCleanupScheduler.class);

    private final ProjectTemplateRepository projectTemplateRepository;
    private final UserGroupRepository userGroupRepository;
    private final UserGroupMemberRepository userGroupMemberRepository;
    private final GroupService groupService;

    public GroupCleanupScheduler(
            ProjectTemplateRepository projectTemplateRepository,
            UserGroupRepository userGroupRepository,
            UserGroupMemberRepository userGroupMemberRepository,
            GroupService groupService) {
        this.projectTemplateRepository = projectTemplateRepository;
        this.userGroupRepository = userGroupRepository;
        this.userGroupMemberRepository = userGroupMemberRepository;
        this.groupService = groupService;
    }

    /**
     * Runs every night at midnight.
     * Exposed as package-private so tests can invoke it directly without reflection.
     */
    @Scheduled(cron = "0 0 0 * * ?")
    void disbandAdvisorlessGroupsAfterDeadline() {
        log.info("[GroupCleanupScheduler] Starting nightly advisor-less group cleanup...");

        boolean deadlinePassed = isAnyActiveTemplateDeadlinePassed();
        if (!deadlinePassed) {
            log.info("[GroupCleanupScheduler] No active template deadline has passed. Skipping cleanup.");
            return;
        }

        List<UserGroup> allGroups = userGroupRepository.findAll();
        int disbanded = 0;

        for (UserGroup group : allGroups) {
            boolean hasAdvisor = hasAcceptedAdvisor(group.getId());
            if (!hasAdvisor) {
                log.warn("[GroupCleanupScheduler] Disbanding advisor-less group id={} name='{}'",
                        group.getId(), group.getGroupName());
                groupService.systemDeleteGroup(group.getId());
                disbanded++;
            }
        }

        log.info("[GroupCleanupScheduler] Cleanup complete. Disbanded {} group(s).", disbanded);
    }

    /**
     * Returns true if at least one active template has a non-null
     * {@code groupFormationDeadline} that is strictly before today.
     */
    boolean isAnyActiveTemplateDeadlinePassed() {
        List<ProjectTemplate> activeTemplates = projectTemplateRepository.findByActiveTrue();
        for (ProjectTemplate template : activeTemplates) {
            LocalDate deadline = template.getGroupFormationDeadline();
            if (deadline != null && LocalDate.now().isAfter(deadline)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if the given group has at least one ACCEPTED member whose
     * user role is PROFESSOR or COORDINATOR (i.e., an advisor is present).
     */
    boolean hasAcceptedAdvisor(Long groupId) {
        List<UserGroupMember> acceptedMembers =
                userGroupMemberRepository.findByGroupIdAndStatus(groupId, GroupInviteStatus.ACCEPTED);
        return acceptedMembers.stream()
                .anyMatch(m -> m.getUser().getRole() == Role.PROFESSOR
                        || m.getUser().getRole() == Role.COORDINATOR);
    }
}
