package com.seniorapp.service;

import com.seniorapp.dto.project.StoryPointDtos.*;
import com.seniorapp.entity.GroupInviteStatus;
import com.seniorapp.entity.Project;
import com.seniorapp.entity.ProjectStudentStoryPoint;
import com.seniorapp.entity.Role;
import com.seniorapp.entity.User;
import com.seniorapp.entity.UserGroupMember;
import com.seniorapp.repository.ProjectRepository;
import com.seniorapp.repository.ProjectStudentStoryPointRepository;
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
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProjectStoryPointService {

    private static final double MAX_STORY_POINTS = 1_000_000.0;

    private final ProjectCommitteeGradeAccessService committeeGradeAccessService;
    private final ProjectRepository projectRepository;
    private final ProjectStudentStoryPointRepository storyPointRepository;
    private final UserGroupMemberRepository userGroupMemberRepository;

    public ProjectStoryPointService(
            ProjectCommitteeGradeAccessService committeeGradeAccessService,
            ProjectRepository projectRepository,
            ProjectStudentStoryPointRepository storyPointRepository,
            UserGroupMemberRepository userGroupMemberRepository) {
        this.committeeGradeAccessService = committeeGradeAccessService;
        this.projectRepository = projectRepository;
        this.storyPointRepository = storyPointRepository;
        this.userGroupMemberRepository = userGroupMemberRepository;
    }

    /** Not read-only: some JDBC stacks mis-handle strict read-only + mixed repository access. */
    @Transactional
    public StoryPointsListResponse list(Long projectId, Long groupId, User viewer) {
        committeeGradeAccessService.requireGroupAssignedToProject(groupId, projectId);
        committeeGradeAccessService.requireStoryPointEditor(viewer, projectId, groupId);

        List<StudentStoryPointRowDto> rows = buildRows(projectId, groupId);
        StoryPointsPayload payload = new StoryPointsPayload();
        payload.setRows(rows);
        payload.setEditable(true);
        return new StoryPointsListResponse("success", payload);
    }

    @Transactional
    public StoryPointsListResponse save(Long projectId, Long groupId, User editor, SaveStoryPointsRequest request) {
        committeeGradeAccessService.requireGroupAssignedToProject(groupId, projectId);
        committeeGradeAccessService.requireStoryPointEditor(editor, projectId, groupId);

        Set<Long> allowed = allowedStudentUserIds(groupId);
        if (allowed.isEmpty()) {
            throw new IllegalArgumentException("Bu grupta kayıtlı öğrenci bulunamadı.");
        }

        Project project =
                projectRepository.findById(projectId).orElseThrow(() -> new AccessDeniedException("Proje bulunamadı."));

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
                            .findByProject_IdAndStudentUserId(projectId, sid)
                            .orElseGet(
                                    () -> {
                                        ProjectStudentStoryPoint n = new ProjectStudentStoryPoint();
                                        n.setProject(project);
                                        n.setStudentUserId(sid);
                                        return n;
                                    });
            row.setStoryPoints(sp);
            row.setUpdatedByUserId(editorId);
            storyPointRepository.save(row);
        }

        return list(projectId, groupId, editor);
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

    private List<StudentStoryPointRowDto> buildRows(Long projectId, Long groupId) {
        List<UserGroupMember> members = userGroupMemberRepository.findByGroupIdAndStatus(groupId, GroupInviteStatus.ACCEPTED);
        List<UserGroupMember> students =
                members.stream()
                        .filter(m -> m.getUser() != null && m.getUser().getRole() == Role.STUDENT)
                        .collect(Collectors.toList());

        Map<Long, Double> existing =
                storyPointRepository.findByProject_Id(projectId).stream()
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
            dto.setStoryPoints(existing.get(u.getId()));
            rows.add(dto);
        }

        rows.sort(
                Comparator.comparing(
                        StudentStoryPointRowDto::getFullName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        return rows;
    }
}
