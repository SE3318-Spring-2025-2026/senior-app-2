package com.seniorapp.service;

import com.seniorapp.dto.GroupCreateDto;
import com.seniorapp.entity.GroupInviteStatus;
import com.seniorapp.entity.GroupMembershipRole;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the hard schedule validation logic in GroupService.
 *
 * <p>Tests verify that:
 * <ul>
 *   <li>createGroup() is blocked after the group formation deadline has passed.</li>
 *   <li>inviteMember() is blocked after the group formation deadline has passed.</li>
 *   <li>respondInvite() blocks ACCEPT but allows DECLINE after the deadline.</li>
 *   <li>All three operations are permitted when no active template has a deadline set.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GroupService – Hard Schedule Deadline Validation Tests")
class GroupServiceDeadlineTest {

    @Mock private UserGroupRepository userGroupRepository;
    @Mock private UserGroupMemberRepository userGroupMemberRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProjectGroupAssignmentRepository projectGroupAssignmentRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectTemplateRepository projectTemplateRepository;
    @Mock private TemplateCommitteeProfessorRepository templateCommitteeProfessorRepository;
    @Mock private ProjectService projectService;
    @Mock private IntegrationCredentialCryptoService cryptoService;

    @InjectMocks
    private GroupService groupService;

    private User studentUser;
    private User professorUser;
    private UserGroup existingGroup;

    @BeforeEach
    void setUp() {
        studentUser = new User();
        studentUser.setId(1L);
        studentUser.setRole(Role.STUDENT);
        studentUser.setFullName("Test Student");
        studentUser.setEmail("student@test.com");

        professorUser = new User();
        professorUser.setId(2L);
        professorUser.setRole(Role.PROFESSOR);
        professorUser.setFullName("Test Professor");
        professorUser.setEmail("professor@test.com");

        existingGroup = new UserGroup();
        existingGroup.setId(10L);
        existingGroup.setGroupName("Test Group");
        existingGroup.setTeamLeader(studentUser);
    }

    // --- Helper: build a template with a deadline ---

    private ProjectTemplate templateWithDeadline(LocalDate deadline) {
        ProjectTemplate t = new ProjectTemplate();
        t.setId(1L);
        t.setName("Test Template");
        t.setGroupFormationDeadline(deadline);
        t.setActive(true);
        return t;
    }

    // ═══════════════════════════════════════════════════════
    // createGroup() tests
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("createGroup – rejected when deadline has passed (yesterday)")
    void createGroup_deadlinePassed_throws403() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        when(projectTemplateRepository.findByActiveTrue())
                .thenReturn(List.of(templateWithDeadline(yesterday)));

        GroupCreateDto dto = new GroupCreateDto();
        dto.setGroupName("New Group");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> groupService.createGroup(dto, studentUser.getId()));

        assertEquals(403, ex.getStatusCode().value());
        assertTrue(ex.getReason().contains("süresi dolmuştur") || ex.getReason().contains("dolmuştur"));
    }

    @Test
    @DisplayName("createGroup – allowed when deadline is today (inclusive)")
    void createGroup_deadlineToday_allowed() {
        LocalDate today = LocalDate.now();
        when(projectTemplateRepository.findByActiveTrue())
                .thenReturn(List.of(templateWithDeadline(today)));
        when(userGroupRepository.existsByGroupName(anyString())).thenReturn(false);
        when(userRepository.findById(studentUser.getId())).thenReturn(Optional.of(studentUser));
        when(userGroupRepository.save(any())).thenReturn(existingGroup);
        when(userGroupMemberRepository.save(any())).thenReturn(new UserGroupMember());

        GroupCreateDto dto = new GroupCreateDto();
        dto.setGroupName("New Group");

        assertDoesNotThrow(() -> groupService.createGroup(dto, studentUser.getId()));
    }

    @Test
    @DisplayName("createGroup – allowed when no template has a deadline configured")
    void createGroup_noDeadlineConfigured_allowed() {
        ProjectTemplate noDeadlineTemplate = templateWithDeadline(null);
        when(projectTemplateRepository.findByActiveTrue())
                .thenReturn(List.of(noDeadlineTemplate));
        when(userGroupRepository.existsByGroupName(anyString())).thenReturn(false);
        when(userRepository.findById(studentUser.getId())).thenReturn(Optional.of(studentUser));
        when(userGroupRepository.save(any())).thenReturn(existingGroup);
        when(userGroupMemberRepository.save(any())).thenReturn(new UserGroupMember());

        GroupCreateDto dto = new GroupCreateDto();
        dto.setGroupName("New Group");

        assertDoesNotThrow(() -> groupService.createGroup(dto, studentUser.getId()));
    }

    @Test
    @DisplayName("createGroup – allowed when no active templates exist")
    void createGroup_noActiveTemplates_allowed() {
        when(projectTemplateRepository.findByActiveTrue()).thenReturn(List.of());
        when(userGroupRepository.existsByGroupName(anyString())).thenReturn(false);
        when(userRepository.findById(studentUser.getId())).thenReturn(Optional.of(studentUser));
        when(userGroupRepository.save(any())).thenReturn(existingGroup);
        when(userGroupMemberRepository.save(any())).thenReturn(new UserGroupMember());

        GroupCreateDto dto = new GroupCreateDto();
        dto.setGroupName("New Group");

        assertDoesNotThrow(() -> groupService.createGroup(dto, studentUser.getId()));
    }

    // ═══════════════════════════════════════════════════════
    // inviteMember() tests
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("inviteMember – rejected when deadline has passed")
    void inviteMember_deadlinePassed_throws403() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        when(projectTemplateRepository.findByActiveTrue())
                .thenReturn(List.of(templateWithDeadline(yesterday)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> groupService.inviteMember(existingGroup.getId(), professorUser.getId(), studentUser.getId()));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("inviteMember – allowed when deadline is in the future")
    void inviteMember_deadlineInFuture_allowed() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        when(projectTemplateRepository.findByActiveTrue())
                .thenReturn(List.of(templateWithDeadline(tomorrow)));
        when(userGroupRepository.findById(existingGroup.getId())).thenReturn(Optional.of(existingGroup));
        when(userRepository.findById(studentUser.getId())).thenReturn(Optional.of(studentUser));
        when(userRepository.findById(professorUser.getId())).thenReturn(Optional.of(professorUser));
        when(userGroupMemberRepository.existsByGroupIdAndUserIdAndStatusIn(anyLong(), anyLong(), anyList()))
                .thenReturn(true); // student is a member
        when(userGroupMemberRepository.existsByGroupIdAndUserIdAndStatus(anyLong(), anyLong(), any()))
                .thenReturn(false);
        when(userGroupMemberRepository.findByUserIdAndStatusOrderByCreatedAtDesc(anyLong(), any()))
                .thenReturn(List.of());
        when(userGroupMemberRepository.save(any())).thenReturn(new UserGroupMember());

        assertDoesNotThrow(() ->
                groupService.inviteMember(existingGroup.getId(), professorUser.getId(), studentUser.getId()));
    }

    // ═══════════════════════════════════════════════════════
    // respondInvite() tests
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("respondInvite ACCEPT – rejected when deadline has passed")
    void respondInvite_accept_deadlinePassed_throws403() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        when(projectTemplateRepository.findByActiveTrue())
                .thenReturn(List.of(templateWithDeadline(yesterday)));

        UserGroupMember pendingInvite = new UserGroupMember();
        pendingInvite.setId(99L);
        pendingInvite.setUser(studentUser);
        pendingInvite.setStatus(GroupInviteStatus.PENDING);
        pendingInvite.setRole(GroupMembershipRole.MEMBER);

        when(userGroupMemberRepository.findByIdAndUserId(99L, studentUser.getId()))
                .thenReturn(Optional.of(pendingInvite));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> groupService.respondInvite(99L, "ACCEPT", studentUser.getId()));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    @DisplayName("respondInvite DECLINE – always allowed even after deadline")
    void respondInvite_decline_deadlinePassed_allowed() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        // Deadline check is NOT triggered for DECLINE, so we don't need to stub it
        // (or we can stub to return the past deadline – only ACCEPT calls the validator)

        UserGroupMember pendingInvite = new UserGroupMember();
        pendingInvite.setId(99L);
        pendingInvite.setUser(studentUser);
        pendingInvite.setStatus(GroupInviteStatus.PENDING);
        pendingInvite.setRole(GroupMembershipRole.MEMBER);

        when(userGroupMemberRepository.findByIdAndUserId(99L, studentUser.getId()))
                .thenReturn(Optional.of(pendingInvite));
        when(userGroupMemberRepository.save(any())).thenReturn(pendingInvite);

        // Should NOT throw, even though deadline might have passed
        assertDoesNotThrow(() -> groupService.respondInvite(99L, "DECLINE", studentUser.getId()));
        assertEquals(GroupInviteStatus.DECLINED, pendingInvite.getStatus());
    }

    // ═══════════════════════════════════════════════════════
    // assertGroupFormationDeadlineNotPassed() – unit tests
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("assertGroupFormationDeadlineNotPassed – passes when deadline is null")
    void assertDeadline_nullDeadline_passes() {
        when(projectTemplateRepository.findByActiveTrue())
                .thenReturn(List.of(templateWithDeadline(null)));

        assertDoesNotThrow(() -> groupService.assertGroupFormationDeadlineNotPassed());
    }

    @Test
    @DisplayName("assertGroupFormationDeadlineNotPassed – passes when deadline is tomorrow")
    void assertDeadline_tomorrow_passes() {
        when(projectTemplateRepository.findByActiveTrue())
                .thenReturn(List.of(templateWithDeadline(LocalDate.now().plusDays(1))));

        assertDoesNotThrow(() -> groupService.assertGroupFormationDeadlineNotPassed());
    }

    @Test
    @DisplayName("assertGroupFormationDeadlineNotPassed – throws 403 when deadline was yesterday")
    void assertDeadline_yesterday_throws403() {
        when(projectTemplateRepository.findByActiveTrue())
                .thenReturn(List.of(templateWithDeadline(LocalDate.now().minusDays(1))));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> groupService.assertGroupFormationDeadlineNotPassed());

        assertEquals(403, ex.getStatusCode().value());
    }
}
