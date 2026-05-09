package com.seniorapp.service;

import com.seniorapp.dto.project.StoryPointDtos.*;
import com.seniorapp.entity.GroupInviteStatus;
import com.seniorapp.entity.Project;
import com.seniorapp.entity.ProjectIssueSyncSnapshot;
import com.seniorapp.entity.ProjectStudentStoryPoint;
import com.seniorapp.entity.Role;
import com.seniorapp.entity.User;
import com.seniorapp.entity.UserGroup;
import com.seniorapp.entity.UserGroupMember;
import com.seniorapp.repository.ProjectIssueSyncSnapshotRepository;
import com.seniorapp.repository.ProjectRepository;
import com.seniorapp.repository.ProjectStudentStoryPointRepository;
import com.seniorapp.repository.UserGroupRepository;
import com.seniorapp.repository.UserGroupMemberRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProjectStoryPointService {

    private static final double MAX_STORY_POINTS = 1_000_000.0;

    private final ProjectCommitteeGradeAccessService committeeGradeAccessService;
    private final ProjectRepository projectRepository;
    private final ProjectStudentStoryPointRepository storyPointRepository;
    private final ProjectIssueSyncSnapshotRepository issueSyncSnapshotRepository;
    private final UserGroupRepository userGroupRepository;
    private final UserGroupMemberRepository userGroupMemberRepository;

    public ProjectStoryPointService(
            ProjectCommitteeGradeAccessService committeeGradeAccessService,
            ProjectRepository projectRepository,
            ProjectStudentStoryPointRepository storyPointRepository,
            ProjectIssueSyncSnapshotRepository issueSyncSnapshotRepository,
            UserGroupRepository userGroupRepository,
            UserGroupMemberRepository userGroupMemberRepository) {
        this.committeeGradeAccessService = committeeGradeAccessService;
        this.projectRepository = projectRepository;
        this.storyPointRepository = storyPointRepository;
        this.issueSyncSnapshotRepository = issueSyncSnapshotRepository;
        this.userGroupRepository = userGroupRepository;
        this.userGroupMemberRepository = userGroupMemberRepository;
    }

    /** Not read-only: some JDBC stacks mis-handle strict read-only + mixed repository access. */
    @Transactional
    public StoryPointsListResponse list(Long projectId, Long groupId, Integer sprintNo, User viewer) {
        requireSprintNo(sprintNo);
        Long safeProjectId = Objects.requireNonNull(projectId, "projectId zorunludur.");
        ensureViewerCanRead(projectId, groupId, viewer);
        committeeGradeAccessService.requireGroupAssignedToProject(groupId, safeProjectId);
        boolean accepted = isAccepted(safeProjectId, sprintNo);
        boolean editable = isAssignedAdvisor(safeProjectId, groupId, viewer) && !accepted;
        List<StudentStoryPointRowDto> rows = buildRows(safeProjectId, groupId, sprintNo);
        UserGroup group = userGroupRepository.findById(Objects.requireNonNull(groupId, "groupId zorunludur.")).orElse(null);
        Long advisorUserId = (group != null && group.getAdvisor() != null) ? group.getAdvisor().getId() : null;
        StoryPointsPayload payload = new StoryPointsPayload();
        payload.setRows(rows);
        payload.setEditable(editable);
        payload.setAccepted(accepted);
        payload.setAcceptEnabled(editable);
        payload.setSprintNo(sprintNo);
        payload.setAdvisorUserId(advisorUserId);
        return new StoryPointsListResponse("success", payload);
    }

    @Transactional
    public StoryPointsListResponse save(Long projectId, Long groupId, Integer sprintNo, User editor, SaveStoryPointsRequest request) {
        requireSprintNo(sprintNo);
        Long safeProjectId = Objects.requireNonNull(projectId, "projectId zorunludur.");
        committeeGradeAccessService.requireGroupAssignedToProject(groupId, safeProjectId);
        requireAssignedAdvisor(safeProjectId, groupId, editor);
        if (isAccepted(safeProjectId, sprintNo)) {
            throw new IllegalArgumentException("Bu sprint için story point kabul edildi, artık değiştirilemez.");
        }

        Set<Long> allowed = allowedStudentUserIds(groupId);
        if (allowed.isEmpty()) {
            throw new IllegalArgumentException("Bu grupta kayıtlı öğrenci bulunamadı.");
        }

        Project project =
                projectRepository.findById(safeProjectId).orElseThrow(() -> new AccessDeniedException("Proje bulunamadı."));

        List<StoryPointEntry> entries = request != null ? request.getEntries() : List.of();
        for (StoryPointEntry entry : entries) {
            if (entry == null || entry.getStudentUserId() == null) {
                throw new IllegalArgumentException("Öğrenci kimliği eksik.");
            }
            if (!allowed.contains(entry.getStudentUserId())) {
                throw new IllegalArgumentException("Geçersiz öğrenci: " + entry.getStudentUserId());
            }
            validateStoryPoints(entry.getStoryPoints());
        }

        Long editorId = editor.getId();
        for (StoryPointEntry entry : entries) {
            Long sid = entry.getStudentUserId();
            Double sp = entry.getStoryPoints();
            ProjectStudentStoryPoint row =
                    storyPointRepository
                            .findByProject_IdAndStudentUserIdAndSprintNo(safeProjectId, sid, sprintNo)
                            .orElseGet(
                                    () -> {
                                        ProjectStudentStoryPoint n = new ProjectStudentStoryPoint();
                                        n.setProject(project);
                                        n.setStudentUserId(sid);
                                        n.setSprintNo(sprintNo);
                                        return n;
                                    });
            row.setStoryPoints(sp);
            row.setUpdatedByUserId(editorId);
            row.setAccepted(false);
            row.setAcceptedByUserId(null);
            row.setAcceptedAt(null);
            storyPointRepository.save(row);
        }

        return list(safeProjectId, groupId, sprintNo, editor);
    }

    @Transactional
    public StoryPointsListResponse accept(Long projectId, Long groupId, Integer sprintNo, User editor) {
        requireSprintNo(sprintNo);
        Long safeProjectId = Objects.requireNonNull(projectId, "projectId zorunludur.");
        committeeGradeAccessService.requireGroupAssignedToProject(groupId, safeProjectId);
        requireAssignedAdvisor(safeProjectId, groupId, editor);
        if (isAccepted(safeProjectId, sprintNo)) {
            return list(safeProjectId, groupId, sprintNo, editor);
        }

        Set<Long> allowedStudents = allowedStudentUserIds(groupId);
        if (allowedStudents.isEmpty()) {
            throw new IllegalArgumentException("Bu grupta kayıtlı öğrenci bulunamadı.");
        }
        Project project = projectRepository.findById(safeProjectId)
                .orElseThrow(() -> new AccessDeniedException("Proje bulunamadı."));
        Map<Long, Double> computedTotals = computeSprintTotalsByStudent(safeProjectId, sprintNo, groupId);
        Long editorId = editor.getId();

        for (Long studentId : allowedStudents) {
            ProjectStudentStoryPoint row = storyPointRepository
                    .findByProject_IdAndStudentUserIdAndSprintNo(safeProjectId, studentId, sprintNo)
                    .orElseGet(() -> {
                        ProjectStudentStoryPoint created = new ProjectStudentStoryPoint();
                        created.setProject(project);
                        created.setStudentUserId(studentId);
                        created.setSprintNo(sprintNo);
                        return created;
                    });
            if (row.getStoryPoints() == null) {
                row.setStoryPoints(computedTotals.getOrDefault(studentId, 0.0));
            }
            row.setAccepted(true);
            row.setAcceptedByUserId(editorId);
            row.setAcceptedAt(java.time.LocalDateTime.now());
            row.setUpdatedByUserId(editorId);
            storyPointRepository.save(row);
        }
        return list(safeProjectId, groupId, sprintNo, editor);
    }

    private void validateStoryPoints(Double sp) {
        if (sp == null) {
            return;
        }
        if (sp.isNaN() || sp.isInfinite() || sp < 0 || sp > MAX_STORY_POINTS) {
            throw new IllegalArgumentException("Story point 0 ile " + (long) MAX_STORY_POINTS + " arasında olmalıdır.");
        }
    }

    private Set<Long> allowedStudentUserIds(Long groupId) {
        List<UserGroupMember> members = userGroupMemberRepository.findByGroupIdAndStatus(groupId, GroupInviteStatus.ACCEPTED);
        Set<Long> ids = new LinkedHashSet<>();
        for (UserGroupMember m : members) {
            User u = m.getUser();
            if (u != null && u.getRole() == Role.STUDENT) {
                ids.add(u.getId());
            }
        }
        return ids;
    }

    private List<StudentStoryPointRowDto> buildRows(Long projectId, Long groupId, Integer sprintNo) {
        List<UserGroupMember> members = userGroupMemberRepository.findByGroupIdAndStatus(groupId, GroupInviteStatus.ACCEPTED);
        List<UserGroupMember> students =
                members.stream()
                        .filter(m -> m.getUser() != null && m.getUser().getRole() == Role.STUDENT)
                        .collect(Collectors.toList());

        Map<Long, Double> computedTotals = computeSprintTotalsByStudent(projectId, sprintNo, groupId);
        Map<Long, Double> existing =
                storyPointRepository.findByProject_IdAndSprintNo(projectId, sprintNo).stream()
                        .filter(r -> r.getStudentUserId() != null)
                        .collect(
                                Collectors.toMap(
                                        ProjectStudentStoryPoint::getStudentUserId,
                                        ProjectStudentStoryPoint::getStoryPoints,
                                        (a, b) -> a,
                                        LinkedHashMap::new));

        List<StudentStoryPointRowDto> rows = new ArrayList<>();
        for (UserGroupMember m : students) {
            User u = m.getUser();
            StudentStoryPointRowDto dto = new StudentStoryPointRowDto();
            dto.setStudentUserId(u.getId());
            dto.setFullName(u.getFullName());
            dto.setEmail(u.getEmail());
            dto.setMembershipRole(m.getRole() != null ? m.getRole().name() : null);
            Double manual = existing.get(u.getId());
            dto.setStoryPoints(manual != null ? manual : computedTotals.get(u.getId()));
            rows.add(dto);
        }

        rows.sort(
                Comparator.comparing(
                        StudentStoryPointRowDto::getFullName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        return rows;
    }

    private void ensureViewerCanRead(Long projectId, Long groupId, User viewer) {
        if (viewer == null) {
            throw new IllegalArgumentException("Oturum açmanız gerekir.");
        }
        if (viewer.getRole() == Role.STUDENT) {
            boolean isStudentInGroup = userGroupMemberRepository.findByGroupIdAndStatus(groupId, GroupInviteStatus.ACCEPTED).stream()
                    .anyMatch(m -> m.getUser() != null
                            && Objects.equals(m.getUser().getId(), viewer.getId())
                            && m.getUser().getRole() == Role.STUDENT);
            if (!isStudentInGroup) {
                throw new AccessDeniedException("Bu gruba ait story point ekranını göremezsiniz.");
            }
            return;
        }
        if (viewer.getRole() == Role.ADMIN
                || viewer.getRole() == Role.COORDINATOR
                || viewer.getRole() == Role.PROFESSOR) {
            return;
        }
        throw new AccessDeniedException("Bu ekran için yetkiniz yok.");
    }

    private boolean isAccepted(Long projectId, Integer sprintNo) {
        return storyPointRepository.findByProject_IdAndSprintNo(projectId, sprintNo).stream()
                .anyMatch(ProjectStudentStoryPoint::isAccepted);
    }

    private void requireAssignedAdvisor(Long projectId, Long groupId, User editor) {
        if (!isAssignedAdvisor(projectId, groupId, editor)) {
            throw new AccessDeniedException("Story point düzenleme ve kabul etme yalnızca grubun advisorına açıktır.");
        }
    }

    private boolean isAssignedAdvisor(Long projectId, Long groupId, User user) {
        if (user == null || user.getId() == null) {
            return false;
        }
        Long safeGroupId = Objects.requireNonNull(groupId, "groupId zorunludur.");
        UserGroup group = userGroupRepository.findById(safeGroupId).orElse(null);
        return group != null
                && group.getAdvisor() != null
                && Objects.equals(group.getAdvisor().getId(), user.getId());
    }

    private void requireSprintNo(Integer sprintNo) {
        if (sprintNo == null || sprintNo <= 0) {
            throw new IllegalArgumentException("sprintNo zorunludur.");
        }
    }

    private Map<Long, Double> computeSprintTotalsByStudent(Long projectId, Integer sprintNo, Long groupId) {
        List<ProjectIssueSyncSnapshot> snapshots = issueSyncSnapshotRepository
                .findByProject_IdAndSprintNoOrderByIssueKeyAsc(projectId, sprintNo);
        if (snapshots.isEmpty()) {
            return Map.of();
        }
        List<UserGroupMember> members = userGroupMemberRepository.findByGroupIdAndStatus(groupId, GroupInviteStatus.ACCEPTED);
        Map<Long, Set<String>> keysByStudent = new LinkedHashMap<>();
        for (UserGroupMember m : members) {
            User u = m.getUser();
            if (u == null || u.getRole() != Role.STUDENT || u.getId() == null) {
                continue;
            }
            Set<String> keys = new LinkedHashSet<>();
            addCandidate(keys, u.getFullName());
            addCandidate(keys, u.getEmail());
            addCandidate(keys, u.getJiraDisplayName());
            addCandidate(keys, u.getJiraEmail());
            keysByStudent.put(u.getId(), keys);
        }
        Map<Long, Double> totals = new LinkedHashMap<>();
        for (ProjectIssueSyncSnapshot snapshot : snapshots) {
            String assignee = normalize(snapshot.getAssignee());
            if (assignee == null) {
                continue;
            }
            double sp = snapshot.getStoryPoints() != null ? snapshot.getStoryPoints() : 0.0;
            for (Map.Entry<Long, Set<String>> e : keysByStudent.entrySet()) {
                if (e.getValue().contains(assignee)) {
                    totals.merge(e.getKey(), sp, (cur, inc) -> (cur != null ? cur : 0.0) + (inc != null ? inc : 0.0));
                    break;
                }
            }
        }
        return totals;
    }

    private void addCandidate(Set<String> bucket, String value) {
        String normalized = normalize(value);
        if (normalized != null) {
            bucket.add(normalized);
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }
}
