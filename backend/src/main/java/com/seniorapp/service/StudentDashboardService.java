package com.seniorapp.service;

import com.seniorapp.dto.student.StudentDashboardDtos.*;
import com.seniorapp.entity.GroupInviteStatus;
import com.seniorapp.entity.Project;
import com.seniorapp.entity.ProjectDeliverable;
import com.seniorapp.entity.ProjectGroupAssignment;
import com.seniorapp.entity.ProjectSprint;
import com.seniorapp.entity.UserGroupMember;
import com.seniorapp.repository.ProjectGroupAssignmentRepository;
import com.seniorapp.repository.ProjectRepository;
import com.seniorapp.repository.UserGroupMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class StudentDashboardService {
    private final UserGroupMemberRepository userGroupMemberRepository;
    private final ProjectGroupAssignmentRepository projectGroupAssignmentRepository;
    private final ProjectRepository projectRepository;

    public StudentDashboardService(
            UserGroupMemberRepository userGroupMemberRepository,
            ProjectGroupAssignmentRepository projectGroupAssignmentRepository,
            ProjectRepository projectRepository
    ) {
        this.userGroupMemberRepository = userGroupMemberRepository;
        this.projectGroupAssignmentRepository = projectGroupAssignmentRepository;
        this.projectRepository = projectRepository;
    }

    @Transactional(readOnly = true)
    public DashboardData getDashboard(Long userId) {
        List<UserGroupMember> acceptedMemberships = userGroupMemberRepository
                .findByUserIdAndStatusOrderByCreatedAtDesc(userId, GroupInviteStatus.ACCEPTED);
        List<Long> groupIds = acceptedMemberships.stream()
                .map(m -> m.getGroup().getId())
                .distinct()
                .toList();

        List<ProjectGroupAssignment> assignments = groupIds.isEmpty()
                ? List.of()
                : projectGroupAssignmentRepository.findByGroupIdInAndActiveTrue(groupIds);
        List<Long> projectIds = assignments.stream()
                .map(a -> a.getProject().getId())
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<Project> projects = projectIds.isEmpty() ? List.of() : projectRepository.findAllById(projectIds);

        DashboardData data = new DashboardData();
        data.setActiveProjects(toActiveProjects(assignments, projects));
        data.setPendingDeliverables(toPendingDeliverables(projects));
        data.setInvitations(toInvites(userGroupMemberRepository
                .findByUserIdAndStatusOrderByCreatedAtDesc(userId, GroupInviteStatus.PENDING)));
        return data;
    }

    @Transactional(readOnly = true)
    public List<ActiveProjectItem> getActiveProjects(Long userId) {
        List<UserGroupMember> acceptedMemberships = userGroupMemberRepository
                .findByUserIdAndStatusOrderByCreatedAtDesc(userId, GroupInviteStatus.ACCEPTED);
        List<Long> groupIds = acceptedMemberships.stream()
                .map(m -> m.getGroup().getId())
                .distinct()
                .toList();
        List<ProjectGroupAssignment> assignments = groupIds.isEmpty()
                ? List.of()
                : projectGroupAssignmentRepository.findByGroupIdInAndActiveTrue(groupIds);
        List<Long> projectIds = assignments.stream().map(a -> a.getProject().getId()).distinct().toList();
        List<Project> projects = projectIds.isEmpty() ? List.of() : projectRepository.findAllById(projectIds);
        return toActiveProjects(assignments, projects);
    }

    private List<ActiveProjectItem> toActiveProjects(List<ProjectGroupAssignment> assignments, List<Project> projects) {
        return assignments.stream().map(assignment -> {
            Project project = projects.stream()
                    .filter(p -> Objects.equals(p.getId(), assignment.getProject().getId()))
                    .findFirst().orElse(null);
            ActiveProjectItem item = new ActiveProjectItem();
            item.setProjectId(assignment.getProject().getId());
            item.setProjectTitle(project != null ? project.getTitle() : "Untitled Project");
            item.setTerm(project != null ? project.getTerm() : null);
            item.setGroupId(assignment.getGroupId());
            return item;
        }).toList();
    }

    private List<PendingDeliverableItem> toPendingDeliverables(List<Project> projects) {
        LocalDate today = LocalDate.now();
        List<PendingDeliverableItem> items = new ArrayList<>();
        for (Project project : projects) {
            for (ProjectSprint sprint : project.getSprints()) {
                for (ProjectDeliverable deliverable : sprint.getDeliverables()) {
                    PendingDeliverableItem item = new PendingDeliverableItem();
                    item.setProjectId(project.getId());
                    item.setProjectTitle(project.getTitle());
                    item.setSprintTitle(sprint.getTitle());
                    item.setDeliverableTitle(deliverable.getTitle());
                    item.setDueDate(sprint.getEndDate());
                    items.add(item);
                }
            }
        }
        return items.stream()
                .filter(i -> i.getDueDate() == null || !i.getDueDate().isBefore(today))
                .sorted(Comparator.comparing(PendingDeliverableItem::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    private List<InviteItem> toInvites(List<UserGroupMember> invites) {
        return invites.stream().map(invite -> {
            InviteItem item = new InviteItem();
            item.setInviteId(invite.getId());
            item.setGroupId(invite.getGroup().getId());
            item.setGroupName(invite.getGroup().getGroupName());
            item.setInvitedByUserId(invite.getInvitedByUserId());
            item.setInvitedAt(invite.getCreatedAt());
            return item;
        }).toList();
    }
}
