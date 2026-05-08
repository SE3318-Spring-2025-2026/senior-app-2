package com.seniorapp.service;

import com.seniorapp.entity.GroupInviteStatus;
import com.seniorapp.entity.ProjectTemplate;
import com.seniorapp.entity.Role;
import com.seniorapp.entity.User;
import com.seniorapp.entity.UserGroup;
import com.seniorapp.entity.UserGroupMember;
import com.seniorapp.repository.ProjectTemplateRepository;
import com.seniorapp.repository.UserGroupMemberRepository;
import com.seniorapp.repository.UserGroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link GroupCleanupScheduler}.
 *
 * <p>Verified behaviours:
 * <ul>
 *   <li>Scheduler does nothing when no active template has a past deadline.</li>
 *   <li>Scheduler does nothing when deadline is today or in the future.</li>
 *   <li>Advisor-less groups are disbanded after the deadline passes.</li>
 *   <li>Groups WITH an accepted advisor are preserved.</li>
 *   <li>Mixed scenario: only advisor-less groups are disbanded.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GroupCleanupScheduler – Advisor-less Group Disbanding Tests")
class GroupCleanupSchedulerTest {

    @Mock private ProjectTemplateRepository projectTemplateRepository;
    @Mock private UserGroupRepository userGroupRepository;
    @Mock private UserGroupMemberRepository userGroupMemberRepository;
    @Mock private GroupService groupService;

    @InjectMocks
    private GroupCleanupScheduler scheduler;

    private User studentUser;
    private User professorUser;

    @BeforeEach
    void setUp() {
        studentUser = new User();
        studentUser.setId(1L);
        studentUser.setRole(Role.STUDENT);

        professorUser = new User();
        professorUser.setId(2L);
        professorUser.setRole(Role.PROFESSOR);
    }

    // --- Helper factories ---

    private ProjectTemplate templateWithDeadline(LocalDate deadline) {
        ProjectTemplate t = new ProjectTemplate();
        t.setId(1L);
        t.setGroupFormationDeadline(deadline);
        t.setActive(true);
        return t;
    }

    private UserGroup group(Long id, String name) {
        UserGroup g = new UserGroup();
        g.setId(id);
        g.setGroupName(name);
        return g;
    }

    private UserGroupMember acceptedMember(User user) {
        UserGroupMember m = new UserGroupMember();
        m.setUser(user);
        m.setStatus(GroupInviteStatus.ACCEPTED);
        return m;
    }

    // ═══════════════════════════════════════════════════════
    // isAnyActiveTemplateDeadlinePassed() tests
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("isAnyActiveTemplateDeadlinePassed – false when no active templates")
    void deadlinePassed_noActiveTemplates_returnsFalse() {
        when(projectTemplateRepository.findByActiveTrue()).thenReturn(List.of());
        assertFalse(scheduler.isAnyActiveTemplateDeadlinePassed());
    }

    @Test
    @DisplayName("isAnyActiveTemplateDeadlinePassed – false when deadline is null")
    void deadlinePassed_nullDeadline_returnsFalse() {
        when(projectTemplateRepository.findByActiveTrue())
                .thenReturn(List.of(templateWithDeadline(null)));
        assertFalse(scheduler.isAnyActiveTemplateDeadlinePassed());
    }

    @Test
    @DisplayName("isAnyActiveTemplateDeadlinePassed – false when deadline is today")
    void deadlinePassed_today_returnsFalse() {
        when(projectTemplateRepository.findByActiveTrue())
                .thenReturn(List.of(templateWithDeadline(LocalDate.now())));
        assertFalse(scheduler.isAnyActiveTemplateDeadlinePassed());
    }

    @Test
    @DisplayName("isAnyActiveTemplateDeadlinePassed – false when deadline is tomorrow")
    void deadlinePassed_tomorrow_returnsFalse() {
        when(projectTemplateRepository.findByActiveTrue())
                .thenReturn(List.of(templateWithDeadline(LocalDate.now().plusDays(1))));
        assertFalse(scheduler.isAnyActiveTemplateDeadlinePassed());
    }

    @Test
    @DisplayName("isAnyActiveTemplateDeadlinePassed – true when deadline was yesterday")
    void deadlinePassed_yesterday_returnsTrue() {
        when(projectTemplateRepository.findByActiveTrue())
                .thenReturn(List.of(templateWithDeadline(LocalDate.now().minusDays(1))));
        assertTrue(scheduler.isAnyActiveTemplateDeadlinePassed());
    }

    // ═══════════════════════════════════════════════════════
    // hasAcceptedAdvisor() tests
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("hasAcceptedAdvisor – false when group has only student members")
    void hasAdvisor_onlyStudents_returnsFalse() {
        when(userGroupMemberRepository.findByGroupIdAndStatus(10L, GroupInviteStatus.ACCEPTED))
                .thenReturn(List.of(acceptedMember(studentUser)));
        assertFalse(scheduler.hasAcceptedAdvisor(10L));
    }

    @Test
    @DisplayName("hasAcceptedAdvisor – true when group has an accepted PROFESSOR")
    void hasAdvisor_professorPresent_returnsTrue() {
        when(userGroupMemberRepository.findByGroupIdAndStatus(10L, GroupInviteStatus.ACCEPTED))
                .thenReturn(List.of(acceptedMember(studentUser), acceptedMember(professorUser)));
        assertTrue(scheduler.hasAcceptedAdvisor(10L));
    }

    @Test
    @DisplayName("hasAcceptedAdvisor – true when group has an accepted COORDINATOR")
    void hasAdvisor_coordinatorPresent_returnsTrue() {
        User coord = new User();
        coord.setId(3L);
        coord.setRole(Role.COORDINATOR);

        when(userGroupMemberRepository.findByGroupIdAndStatus(10L, GroupInviteStatus.ACCEPTED))
                .thenReturn(List.of(acceptedMember(studentUser), acceptedMember(coord)));
        assertTrue(scheduler.hasAcceptedAdvisor(10L));
    }

    @Test
    @DisplayName("hasAcceptedAdvisor – false when group has no members at all")
    void hasAdvisor_noMembers_returnsFalse() {
        when(userGroupMemberRepository.findByGroupIdAndStatus(10L, GroupInviteStatus.ACCEPTED))
                .thenReturn(List.of());
        assertFalse(scheduler.hasAcceptedAdvisor(10L));
    }

    // ═══════════════════════════════════════════════════════
    // disbandAdvisorlessGroupsAfterDeadline() (full flow) tests
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("Full flow – skips all work when no deadline has passed")
    void fullFlow_noDeadlinePassed_skipsCleanup() {
        when(projectTemplateRepository.findByActiveTrue())
                .thenReturn(List.of(templateWithDeadline(LocalDate.now().plusDays(5))));

        scheduler.disbandAdvisorlessGroupsAfterDeadline();

        verify(userGroupRepository, never()).findAll();
        verify(groupService, never()).systemDeleteGroup(anyLong());
    }

    @Test
    @DisplayName("Full flow – disbands advisor-less group after deadline")
    void fullFlow_deadlinePassed_advisorlessGroupDisbanded() {
        when(projectTemplateRepository.findByActiveTrue())
                .thenReturn(List.of(templateWithDeadline(LocalDate.now().minusDays(1))));

        UserGroup advisorlessGroup = group(20L, "Advisor-less Group");
        when(userGroupRepository.findAll()).thenReturn(List.of(advisorlessGroup));
        when(userGroupMemberRepository.findByGroupIdAndStatus(20L, GroupInviteStatus.ACCEPTED))
                .thenReturn(List.of(acceptedMember(studentUser))); // no advisor

        scheduler.disbandAdvisorlessGroupsAfterDeadline();

        verify(groupService, times(1)).systemDeleteGroup(20L);
    }

    @Test
    @DisplayName("Full flow – preserves group that already has an advisor")
    void fullFlow_deadlinePassed_groupWithAdvisorPreserved() {
        when(projectTemplateRepository.findByActiveTrue())
                .thenReturn(List.of(templateWithDeadline(LocalDate.now().minusDays(1))));

        UserGroup advisedGroup = group(30L, "Advised Group");
        when(userGroupRepository.findAll()).thenReturn(List.of(advisedGroup));
        when(userGroupMemberRepository.findByGroupIdAndStatus(30L, GroupInviteStatus.ACCEPTED))
                .thenReturn(List.of(acceptedMember(studentUser), acceptedMember(professorUser)));

        scheduler.disbandAdvisorlessGroupsAfterDeadline();

        verify(groupService, never()).systemDeleteGroup(anyLong());
    }

    @Test
    @DisplayName("Full flow – mixed: disbands only advisor-less, keeps advised groups")
    void fullFlow_mixed_onlyAdvisorlessDisbanded() {
        when(projectTemplateRepository.findByActiveTrue())
                .thenReturn(List.of(templateWithDeadline(LocalDate.now().minusDays(3))));

        UserGroup advisorlessGroup = group(41L, "No Advisor");
        UserGroup advisedGroup     = group(42L, "Has Advisor");
        when(userGroupRepository.findAll()).thenReturn(List.of(advisorlessGroup, advisedGroup));

        when(userGroupMemberRepository.findByGroupIdAndStatus(41L, GroupInviteStatus.ACCEPTED))
                .thenReturn(List.of(acceptedMember(studentUser)));
        when(userGroupMemberRepository.findByGroupIdAndStatus(42L, GroupInviteStatus.ACCEPTED))
                .thenReturn(List.of(acceptedMember(studentUser), acceptedMember(professorUser)));

        scheduler.disbandAdvisorlessGroupsAfterDeadline();

        verify(groupService, times(1)).systemDeleteGroup(41L);
        verify(groupService, never()).systemDeleteGroup(42L);
    }
}
