package com.seniorapp.service;

import com.seniorapp.dto.GroupCreateDto;
import com.seniorapp.dto.GroupIntegrationsRequest;
import com.seniorapp.dto.GroupIntegrationsResponse;
import com.seniorapp.dto.GroupMemberActionDto;
import com.seniorapp.dto.GroupInviteRespondResultDto;
import com.seniorapp.dto.TeamManagementDtos.CreateProjectFromTemplateRequest;
import com.seniorapp.dto.TeamManagementDtos.MemberDto;
import com.seniorapp.dto.TeamManagementDtos.ProjectLinkDto;
import com.seniorapp.dto.TeamManagementDtos.StudentOptionDto;
import com.seniorapp.dto.TeamManagementDtos.TeamDto;
import com.seniorapp.dto.student.StudentDashboardDtos.InviteItem;
import com.seniorapp.dto.project.ProjectDtos.CreateProjectRequest;
import com.seniorapp.entity.GroupInviteStatus;
import com.seniorapp.entity.GroupMembershipRole;
import com.seniorapp.entity.Project;
import com.seniorapp.entity.ProjectCommittee;
import com.seniorapp.entity.ProjectCommitteeProfessor;
import com.seniorapp.entity.ProjectGroupAssignment;
import com.seniorapp.entity.ProjectTemplate;
import com.seniorapp.entity.Role;
import com.seniorapp.entity.User;
import com.seniorapp.entity.UserGroup;
import com.seniorapp.entity.UserGroupMember;
import com.seniorapp.repository.ProjectGroupAssignmentRepository;
import com.seniorapp.repository.ProjectCommitteeProfessorRepository;
import com.seniorapp.repository.ProjectRepository;
import com.seniorapp.repository.ProjectTemplateRepository;
import com.seniorapp.repository.UserGroupMemberRepository;
import com.seniorapp.repository.UserGroupRepository;
import com.seniorapp.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GroupService {
    private final UserGroupRepository userGroupRepository;
    private final UserGroupMemberRepository userGroupMemberRepository;
    private final UserRepository userRepository;
    private final ProjectCommitteeProfessorRepository projectCommitteeProfessorRepository;
    private final ProjectGroupAssignmentRepository projectGroupAssignmentRepository;
    private final ProjectRepository projectRepository;
    private final ProjectTemplateRepository projectTemplateRepository;
    private final ProjectService projectService;
    private final IntegrationCredentialCryptoService cryptoService;

    public GroupService(UserGroupRepository userGroupRepository,
                        UserGroupMemberRepository userGroupMemberRepository,
                        UserRepository userRepository,
                        ProjectCommitteeProfessorRepository projectCommitteeProfessorRepository,
                        ProjectGroupAssignmentRepository projectGroupAssignmentRepository,
                        ProjectRepository projectRepository,
                        ProjectTemplateRepository projectTemplateRepository,
                        ProjectService projectService,
                        IntegrationCredentialCryptoService cryptoService) {
        this.userGroupRepository = userGroupRepository;
        this.userGroupMemberRepository = userGroupMemberRepository;
        this.userRepository = userRepository;
        this.projectCommitteeProfessorRepository = projectCommitteeProfessorRepository;
        this.projectGroupAssignmentRepository = projectGroupAssignmentRepository;
        this.projectRepository = projectRepository;
        this.projectTemplateRepository = projectTemplateRepository;
        this.projectService = projectService;
        this.cryptoService = cryptoService;
    }

    // --- ISSUE #71 & #72 FIX: GERÇEK GRUP KURMA ---
    public void createGroup(GroupCreateDto dto, Long currentUserId) {
        if (dto.getGroupName() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Group name is required.");
        }

        // 1. Conflict Kontrolü (Aynı isimde grup var mı?)
        if (userGroupRepository.existsByGroupName(dto.getGroupName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Group name is already taken.");
        }

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));

        // 2. Grubu kur ve lideri ata (artık bir kullanıcı birden fazla grupta olabilir)
        UserGroup group = new UserGroup();
        group.setGroupName(dto.getGroupName());
        group.setTeamLeader(currentUser);
        group.setMemberships(new ArrayList<>());
        UserGroup savedGroup = userGroupRepository.save(group);

        UserGroupMember leaderMembership = new UserGroupMember();
        leaderMembership.setGroup(savedGroup);
        leaderMembership.setUser(currentUser);
        leaderMembership.setRole(GroupMembershipRole.LEADER);
        leaderMembership.setStatus(GroupInviteStatus.ACCEPTED);
        leaderMembership.setInvitedByUserId(currentUserId);
        userGroupMemberRepository.save(leaderMembership);
    }

    // Existing member API now works on invite/membership records.
    public void manageMembership(Long groupId, GroupMemberActionDto dto, Long currentUserId) {
        UserGroup group = userGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found."));

        // Yetki Kontrolü: İsteği atan kişi takım lideri mi? (BUG #2 FIX: 403 döner)
        if (group.getTeamLeader() == null || !group.getTeamLeader().getId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the Team Leader can manage members.");
        }

        User targetUser = userRepository.findById(Long.valueOf(dto.getStudentId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Target student not found."));

        if ("ADD".equalsIgnoreCase(dto.getAction())) {
            inviteMember(groupId, targetUser.getId(), currentUserId);
        } else if ("REMOVE".equalsIgnoreCase(dto.getAction())) {
            UserGroupMember membership = userGroupMemberRepository
                    .findByGroupIdAndUserIdAndStatus(groupId, targetUser.getId(), GroupInviteStatus.ACCEPTED)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Student is not in this group."));
            userGroupMemberRepository.delete(membership);
        }
    }

    public void inviteMember(Long groupId, Long studentUserId, Long currentUserId) {
        UserGroup group = userGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found."));

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));

        boolean isProfessorOrCoordinator = currentUser.getRole() == Role.PROFESSOR || currentUser.getRole() == Role.COORDINATOR;

        if (!isProfessorOrCoordinator) {
            boolean isGroupMember = userGroupMemberRepository
                    .existsByGroupIdAndUserIdAndStatusIn(groupId, currentUserId,
                        List.of(GroupInviteStatus.ACCEPTED));
            if (!isGroupMember) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You must be a group member to invite others.");
            }
        }

        User targetUser = userRepository.findById(studentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Target user not found."));

        // Only team leader can invite students, but any group member can invite professors
        boolean isLeader = group.getTeamLeader() != null && group.getTeamLeader().getId().equals(currentUserId);
        boolean isTargetStudent = targetUser.getRole() == Role.STUDENT;
        boolean isCurrentUserStudent = currentUser.getRole() == Role.STUDENT;
        boolean isTargetProfessor = targetUser.getRole() == Role.PROFESSOR;

        if (isTargetStudent && !isLeader && !isProfessorOrCoordinator) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only Team Leader can invite students.");
        }
        
        // Students can invite professors, professors can invite students
        if (isCurrentUserStudent && !isTargetStudent && !isLeader) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only Team Leader can invite professors.");
        }
        if (isTargetProfessor) {
            ProjectGroupAssignment assignment = projectGroupAssignmentRepository.findByGroupIdAndActiveTrue(groupId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Advisor invite is available only after the group is assigned to a project."));
            Long projectId = assignment.getProject() != null ? assignment.getProject().getId() : null;
            if (projectId == null || !projectCommitteeProfessorRepository.existsByCommittee_Project_IdAndProfessor_Id(projectId, targetUser.getId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Selected advisor is not in this project's committees.");
            }
        }

        if (userGroupMemberRepository.existsByGroupIdAndUserIdAndStatus(groupId, studentUserId, GroupInviteStatus.ACCEPTED)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already in this group.");
        }

        List<UserGroupMember> existingInvites = userGroupMemberRepository
                .findByUserIdAndStatusOrderByCreatedAtDesc(studentUserId, GroupInviteStatus.PENDING);
        boolean alreadyPendingForSameGroup = existingInvites.stream()
                .anyMatch(invite -> invite.getGroup().getId().equals(groupId));
        if (alreadyPendingForSameGroup) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already has a pending invite for this group.");
        }

        UserGroupMember invite = new UserGroupMember();
        invite.setGroup(group);
        invite.setUser(targetUser);
        invite.setRole(GroupMembershipRole.MEMBER);
        invite.setStatus(GroupInviteStatus.PENDING);
        invite.setInvitedByUserId(currentUserId);
        userGroupMemberRepository.save(invite);
    }

    @Transactional
    public GroupInviteRespondResultDto respondInvite(Long inviteId, String action, Long committeeId, Long currentUserId) {
        UserGroupMember invite = userGroupMemberRepository.findByIdAndUserId(inviteId, currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invite not found."));
        if (invite.getStatus() != GroupInviteStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invite already processed.");
        }
        GroupInviteRespondResultDto result = new GroupInviteRespondResultDto();
        if ("ACCEPT".equalsIgnoreCase(action)) {
            if (invite.getUser().getRole() == Role.PROFESSOR) {
                return processAdvisorAccept(invite, committeeId);
            }
            invite.setStatus(GroupInviteStatus.ACCEPTED);
            result.setMessage("Invite accepted.");
        } else if ("DECLINE".equalsIgnoreCase(action)) {
            invite.setStatus(GroupInviteStatus.DECLINED);
            result.setMessage("Invite declined.");
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid action. Use ACCEPT or DECLINE.");
        }
        invite.setRespondedAt(java.time.LocalDateTime.now());
        userGroupMemberRepository.save(invite);
        return result;
    }

    private GroupInviteRespondResultDto processAdvisorAccept(UserGroupMember invite, Long committeeId) {
        GroupInviteRespondResultDto result = new GroupInviteRespondResultDto();
        Long groupId = invite.getGroup().getId();
        Long advisorUserId = invite.getUser().getId();
        ProjectGroupAssignment assignment = projectGroupAssignmentRepository.findByGroupIdAndActiveTrue(groupId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Group is not assigned to any active project."));
        Long projectId = assignment.getProject() != null ? assignment.getProject().getId() : null;
        if (projectId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project not found for this group.");
        }

        List<ProjectCommitteeProfessor> committeeMemberships =
                projectCommitteeProfessorRepository.findByProjectIdAndProfessorId(projectId, advisorUserId);
        if (committeeMemberships.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Advisor is not a member of any committee in this project.");
        }

        ProjectCommittee resolvedCommittee;
        if (committeeMemberships.size() == 1) {
            resolvedCommittee = committeeMemberships.get(0).getCommittee();
            result.setAutoAssigned(true);
        } else {
            if (committeeId == null) {
                result.setSelectionRequired(true);
                result.setMessage("Committee selection required.");
                result.setCommitteeOptions(committeeMemberships.stream()
                        .map(ProjectCommitteeProfessor::getCommittee)
                        .filter(Objects::nonNull)
                        .map(c -> toCommitteeOption(c, projectId))
                        .toList());
                return result;
            }
            resolvedCommittee = committeeMemberships.stream()
                    .map(ProjectCommitteeProfessor::getCommittee)
                    .filter(Objects::nonNull)
                    .filter(c -> Objects.equals(c.getId(), committeeId))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Selected committee is not valid for this advisor."));
        }

        invite.setStatus(GroupInviteStatus.ACCEPTED);
        invite.setRespondedAt(java.time.LocalDateTime.now());
        userGroupMemberRepository.save(invite);

        assignment.setCommittee(resolvedCommittee);
        projectGroupAssignmentRepository.save(assignment);

        List<UserGroupMember> pendingForSameGroup =
                userGroupMemberRepository.findByUserIdAndGroupIdAndStatus(advisorUserId, groupId, GroupInviteStatus.PENDING);
        for (UserGroupMember pendingInvite : pendingForSameGroup) {
            if (!Objects.equals(pendingInvite.getId(), invite.getId())) {
                pendingInvite.setStatus(GroupInviteStatus.DECLINED);
                pendingInvite.setRespondedAt(java.time.LocalDateTime.now());
                userGroupMemberRepository.save(pendingInvite);
            }
        }

        result.setAssignedCommitteeId(resolvedCommittee.getId());
        result.setAssignedCommitteeName(resolvedCommittee.getName());
        result.setMessage("Invite accepted and committee assigned.");
        return result;
    }

    private GroupInviteRespondResultDto.CommitteeOption toCommitteeOption(ProjectCommittee committee, Long projectId) {
        GroupInviteRespondResultDto.CommitteeOption option = new GroupInviteRespondResultDto.CommitteeOption();
        option.setCommitteeId(committee.getId());
        option.setCommitteeName(committee.getName());
        option.setAssignedGroupCount(
                projectGroupAssignmentRepository.countByProjectIdAndCommittee_IdAndActiveTrue(projectId, committee.getId()));
        return option;
    }

    public List<TeamDto> getMyTeams(Long currentUserId) {
        List<UserGroupMember> memberships = userGroupMemberRepository
                .findByUserIdAndStatusOrderByCreatedAtDesc(currentUserId, GroupInviteStatus.ACCEPTED);
        Set<Long> groupIds = memberships.stream().map(m -> m.getGroup().getId()).collect(Collectors.toSet());
        return groupIds.stream()
                .map(userGroupRepository::findById)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .map(group -> toTeamDto(group, currentUserId))
                .toList();
    }

    public List<TeamDto> getAllTeams(Long currentUserId) {
        return userGroupRepository.findAll().stream()
                .map(group -> toTeamDto(group, currentUserId))
                .toList();
    }

    public List<StudentOptionDto> listStudents() {
        return userRepository.findByRole(Role.STUDENT).stream().map(student -> {
            StudentOptionDto dto = new StudentOptionDto();
            dto.setUserId(student.getId());
            dto.setFullName(student.getFullName());
            dto.setEmail(student.getEmail());
            return dto;
        }).toList();
    }

    public List<StudentOptionDto> listAdvisorOptions(Long groupId, Long currentUserId) {
        UserGroup group = userGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found."));
        boolean isMember = userGroupMemberRepository.existsByGroupIdAndUserIdAndStatusIn(
                groupId, currentUserId, List.of(GroupInviteStatus.ACCEPTED, GroupInviteStatus.PENDING));
        if (!isMember && (group.getTeamLeader() == null || !group.getTeamLeader().getId().equals(currentUserId))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed for this team.");
        }
        ProjectGroupAssignment assignment = projectGroupAssignmentRepository.findByGroupIdAndActiveTrue(groupId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Advisor invite is available only after the group is assigned to a project."));
        Long projectId = assignment.getProject() != null ? assignment.getProject().getId() : null;
        if (projectId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assigned project not found for this group.");
        }
        Set<Long> advisorUserIds = new java.util.LinkedHashSet<>(
                projectCommitteeProfessorRepository.findDistinctProfessorIdsByProjectId(projectId)
        );
        return advisorUserIds.stream()
                .map(userRepository::findById)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .map(user -> {
                    StudentOptionDto dto = new StudentOptionDto();
                    dto.setUserId(user.getId());
                    dto.setFullName(user.getFullName());
                    dto.setEmail(user.getEmail());
                    return dto;
                }).toList();
    }

    public List<InviteItem> getMyInvites(Long currentUserId) {
        return userGroupMemberRepository.findByUserIdAndStatusOrderByCreatedAtDesc(currentUserId, GroupInviteStatus.PENDING)
                .stream()
                .map(invite -> {
                    InviteItem item = new InviteItem();
                    item.setInviteId(invite.getId());
                    item.setGroupId(invite.getGroup().getId());
                    item.setGroupName(invite.getGroup().getGroupName());
                    item.setInvitedByUserId(invite.getInvitedByUserId());
                    item.setInvitedAt(invite.getCreatedAt());
                    return item;
                })
                .toList();
    }

    public Long createProjectForGroup(Long groupId, CreateProjectFromTemplateRequest request, Long currentUserId) {
        if (request == null || request.getTemplateId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "templateId is required.");
        }
        UserGroup group = userGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found."));
        if (group.getTeamLeader() == null || !group.getTeamLeader().getId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only team leader can create project for team.");
        }
        projectGroupAssignmentRepository.findByGroupIdAndActiveTrue(groupId).ifPresent(existing -> {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This team is already linked to a project.");
        });
        ProjectTemplate template = projectTemplateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found."));

        CreateProjectRequest create = new CreateProjectRequest();
        create.setTemplateId(request.getTemplateId());
        create.setGroupId(groupId);
        create.setTitle(group.getGroupName() + " - " + template.getName());
        create.setTerm(template.getTerm());
        return projectService.createProject(create, currentUserId);
    }

    private TeamDto toTeamDto(UserGroup group, Long currentUserId) {
        TeamDto dto = new TeamDto();
        dto.setGroupId(group.getId());
        dto.setGroupName(group.getGroupName());
        dto.setLeaderUserId(group.getTeamLeader() != null ? group.getTeamLeader().getId() : null);
        dto.setCurrentUserLeader(group.getTeamLeader() != null && group.getTeamLeader().getId().equals(currentUserId));

        List<UserGroupMember> allMembers = userGroupMemberRepository.findByGroupIdOrderByCreatedAtAsc(group.getId());
        dto.setMembers(allMembers.stream().map(this::toMemberDto).toList());

        ProjectLinkDto project = null;
        var assignment = projectGroupAssignmentRepository.findByGroupIdAndActiveTrue(group.getId());
        if (assignment.isPresent()) {
            Project p = projectRepository.findById(assignment.get().getProject().getId()).orElse(null);
            if (p != null) {
                project = new ProjectLinkDto();
                project.setProjectId(p.getId());
                project.setTitle(p.getTitle());
                project.setTerm(p.getTerm());
            }
        }
        dto.setProject(project);
        return dto;
    }

    private MemberDto toMemberDto(UserGroupMember membership) {
        MemberDto dto = new MemberDto();
        dto.setUserId(membership.getUser().getId());
        dto.setFullName(membership.getUser().getFullName());
        dto.setEmail(membership.getUser().getEmail());
        dto.setRole(membership.getRole().name());
        dto.setInviteStatus(membership.getStatus().name());
        return dto;
    }

    public String getMyRoleInGroup(Long groupId, Long userId) {
        UserGroupMember membership = userGroupMemberRepository
                .findByGroupIdAndUserIdAndStatus(groupId, userId, GroupInviteStatus.ACCEPTED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bu grubun üyesi değilsiniz."));
        return membership.getRole().name();
    }

    public void saveIntegrations(Long groupId, GroupIntegrationsRequest request) {
        validateJiraUrl(request.getJiraSpaceUrl());

        UserGroup group = userGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found."));

        // Persist plaintext values here; JPA lifecycle listener encrypts before INSERT/UPDATE.
        group.setGithubPatEncrypted(request.getGithubPat().trim());
        group.setJiraSpaceUrlEncrypted(request.getJiraSpaceUrl().trim());
        userGroupRepository.save(group);
    }

    public GroupIntegrationsResponse getIntegrations(Long groupId) {
        UserGroup group = userGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found."));

        String decryptedJiraUrl = cryptoService.decrypt(group.getJiraSpaceUrlEncrypted());

        // PAT is intentionally never sent back in plaintext to API clients.
        return new GroupIntegrationsResponse("", decryptedJiraUrl);
    }

    private void validateJiraUrl(String jiraSpaceUrl) {
        try {
            URI uri = new URI(jiraSpaceUrl.trim());
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (host == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JIRA workspace URL format.");
            }
        } catch (URISyntaxException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid JIRA workspace URL format.");
        }
    }

    @Transactional
    public void deleteGroup(Long groupId, Long currentUserId) {
        UserGroup group = userGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found."));

        // Only the team leader can delete the group
        if (group.getTeamLeader() == null || !group.getTeamLeader().getId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the Team Leader can delete the group.");
        }

        projectGroupAssignmentRepository.findByGroupIdAndActiveTrue(groupId).ifPresent(assignment -> {
            assignment.setActive(false);
            projectGroupAssignmentRepository.save(assignment);

            Project project = assignment.getProject();
            if (project != null && groupId.equals(project.getGroupId())) {
                project.setGroupId(null);
                projectRepository.save(project);
            }
        });

        // Delete all group memberships
        userGroupMemberRepository.deleteByGroupId(groupId);

        // Delete the group
        userGroupRepository.delete(group);
    }
}
