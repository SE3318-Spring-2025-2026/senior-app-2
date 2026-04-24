package com.seniorapp.service;

import com.seniorapp.dto.GroupCreateDto;
import com.seniorapp.dto.GroupIntegrationsRequest;
import com.seniorapp.dto.GroupIntegrationsResponse;
import com.seniorapp.dto.GroupMemberActionDto;
import com.seniorapp.dto.TeamManagementDtos.CreateProjectFromTemplateRequest;
import com.seniorapp.dto.TeamManagementDtos.MemberDto;
import com.seniorapp.dto.TeamManagementDtos.ProjectLinkDto;
import com.seniorapp.dto.TeamManagementDtos.StudentOptionDto;
import com.seniorapp.dto.TeamManagementDtos.TeamDto;
import com.seniorapp.dto.project.ProjectDtos.CreateProjectRequest;
import com.seniorapp.entity.GroupInviteStatus;
import com.seniorapp.entity.GroupMembershipRole;
import com.seniorapp.entity.Project;
import com.seniorapp.entity.ProjectTemplate;
import com.seniorapp.entity.Role;
import com.seniorapp.entity.User;
import com.seniorapp.entity.UserGroup;
import com.seniorapp.entity.UserGroupMember;
import com.seniorapp.repository.ProjectGroupAssignmentRepository;
import com.seniorapp.repository.ProjectRepository;
import com.seniorapp.repository.ProjectTemplateRepository;
import com.seniorapp.repository.TemplateCommitteeProfessorRepository;
import com.seniorapp.repository.UserGroupMemberRepository;
import com.seniorapp.repository.UserGroupRepository;
import com.seniorapp.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GroupService {
    private final UserGroupRepository userGroupRepository;
    private final UserGroupMemberRepository userGroupMemberRepository;
    private final UserRepository userRepository;
    private final ProjectGroupAssignmentRepository projectGroupAssignmentRepository;
    private final ProjectRepository projectRepository;
    private final ProjectTemplateRepository projectTemplateRepository;
    private final TemplateCommitteeProfessorRepository templateCommitteeProfessorRepository;
    private final ProjectService projectService;
    private final IntegrationCredentialCryptoService cryptoService;

    public GroupService(UserGroupRepository userGroupRepository,
                        UserGroupMemberRepository userGroupMemberRepository,
                        UserRepository userRepository,
                        ProjectGroupAssignmentRepository projectGroupAssignmentRepository,
                        ProjectRepository projectRepository,
                        ProjectTemplateRepository projectTemplateRepository,
                        TemplateCommitteeProfessorRepository templateCommitteeProfessorRepository,
                        ProjectService projectService,
                        IntegrationCredentialCryptoService cryptoService) {
        this.userGroupRepository = userGroupRepository;
        this.userGroupMemberRepository = userGroupMemberRepository;
        this.userRepository = userRepository;
        this.projectGroupAssignmentRepository = projectGroupAssignmentRepository;
        this.projectRepository = projectRepository;
        this.projectTemplateRepository = projectTemplateRepository;
        this.templateCommitteeProfessorRepository = templateCommitteeProfessorRepository;
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
        if (group.getTeamLeader() == null || !group.getTeamLeader().getId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the Team Leader can invite members.");
        }
        User targetUser = userRepository.findById(studentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Target student not found."));

        if (userGroupMemberRepository.existsByGroupIdAndUserIdAndStatus(groupId, studentUserId, GroupInviteStatus.ACCEPTED)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Student is already in this group.");
        }

        List<UserGroupMember> existingInvites = userGroupMemberRepository
                .findByUserIdAndStatusOrderByCreatedAtDesc(studentUserId, GroupInviteStatus.PENDING);
        boolean alreadyPendingForSameGroup = existingInvites.stream()
                .anyMatch(invite -> invite.getGroup().getId().equals(groupId));
        if (alreadyPendingForSameGroup) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Student already has a pending invite for this group.");
        }

        UserGroupMember invite = new UserGroupMember();
        invite.setGroup(group);
        invite.setUser(targetUser);
        invite.setRole(GroupMembershipRole.MEMBER);
        invite.setStatus(GroupInviteStatus.PENDING);
        invite.setInvitedByUserId(currentUserId);
        userGroupMemberRepository.save(invite);
    }

    public void respondInvite(Long inviteId, String action, Long currentUserId) {
        UserGroupMember invite = userGroupMemberRepository.findByIdAndUserId(inviteId, currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invite not found."));
        if (invite.getStatus() != GroupInviteStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invite already processed.");
        }
        if ("ACCEPT".equalsIgnoreCase(action)) {
            invite.setStatus(GroupInviteStatus.ACCEPTED);
        } else if ("DECLINE".equalsIgnoreCase(action)) {
            invite.setStatus(GroupInviteStatus.DECLINED);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid action. Use ACCEPT or DECLINE.");
        }
        invite.setRespondedAt(java.time.LocalDateTime.now());
        userGroupMemberRepository.save(invite);
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
        var assignment = projectGroupAssignmentRepository.findByGroupIdAndActiveTrue(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Team has no linked project."));
        Project project = assignment.getProject();
        if (project == null || project.getTemplate() == null || project.getTemplate().getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project template link is missing.");
        }
        Long templateId = project.getTemplate().getId();
        Set<Long> advisorUserIds = new java.util.LinkedHashSet<>(
                templateCommitteeProfessorRepository.findDistinctProfessorIdsByTemplateId(templateId)
        );
        userRepository.findByRole(Role.COORDINATOR).stream().map(User::getId).forEach(advisorUserIds::add);
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
}