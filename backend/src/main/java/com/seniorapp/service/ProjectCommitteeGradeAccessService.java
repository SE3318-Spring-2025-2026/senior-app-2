package com.seniorapp.service;

import com.seniorapp.entity.Project;
import com.seniorapp.entity.Role;
import com.seniorapp.entity.User;
import com.seniorapp.repository.ProjectCommitteeProfessorRepository;
import com.seniorapp.repository.ProjectGroupAssignmentRepository;
import com.seniorapp.repository.ProjectRepository;
import com.seniorapp.repository.TemplateCommitteeProfessorRepository;
import com.seniorapp.repository.UserGroupRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Ensures deliverable / sprint evaluation grades are only entered by committee members.
 * Membership is satisfied if the grader is on this <strong>project's</strong> committee
 * <em>or</em> on any committee of the project's <strong>template</strong> (same pool as Manage Comitees UI).
 * {@link Role#ADMIN} bypasses committee checks.
 */
@Service
public class ProjectCommitteeGradeAccessService {

    private final ProjectCommitteeProfessorRepository committeeProfessorRepository;
    private final TemplateCommitteeProfessorRepository templateCommitteeProfessorRepository;
    private final ProjectGroupAssignmentRepository projectGroupAssignmentRepository;
    private final ProjectRepository projectRepository;
    private final UserGroupRepository userGroupRepository;

    public ProjectCommitteeGradeAccessService(
            ProjectCommitteeProfessorRepository committeeProfessorRepository,
            TemplateCommitteeProfessorRepository templateCommitteeProfessorRepository,
            ProjectGroupAssignmentRepository projectGroupAssignmentRepository,
            ProjectRepository projectRepository,
            UserGroupRepository userGroupRepository) {
        this.committeeProfessorRepository = committeeProfessorRepository;
        this.templateCommitteeProfessorRepository = templateCommitteeProfessorRepository;
        this.projectGroupAssignmentRepository = projectGroupAssignmentRepository;
        this.projectRepository = projectRepository;
        this.userGroupRepository = userGroupRepository;
    }

    /**
     * Proje komitesi veya proje şablonunun herhangi bir komitesinde bu öğretim üyesi var mı?
     * {@link Role#ADMIN} için burada true dönülmez; üst katman ayrı ele alır.
     */
    public boolean isCommitteeMemberForProject(User grader, Long projectId) {
        if (grader == null || projectId == null) {
            return false;
        }
        if (committeeProfessorRepository.existsByCommittee_Project_IdAndProfessor_Id(projectId, grader.getId())) {
            return true;
        }
        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) {
            return false;
        }
        Long templateId = project.getTemplate() != null ? project.getTemplate().getId() : null;
        return templateId != null
                && templateCommitteeProfessorRepository.existsByCommittee_Template_IdAndProfessor_Id(
                        templateId, grader.getId());
    }

    public boolean isGroupCoordinator(User user, Long groupId) {
        if (user == null || groupId == null) {
            return false;
        }
        return userGroupRepository
                .findById(groupId)
                .map(g -> g.getCoordinator() != null && Objects.equals(g.getCoordinator().getId(), user.getId()))
                .orElse(false);
    }

    /**
     * Komite üyesi (proje veya şablon) veya grubun atanmış koordinatörü veya admin.
     */
    public void requireStoryPointEditor(User editor, Long projectId, Long groupId) {
        if (editor == null) {
            throw new IllegalArgumentException("Oturum açmanız gerekir.");
        }
        if (editor.getRole() == Role.ADMIN) {
            return;
        }
        if (projectId == null) {
            throw new IllegalArgumentException("Proje bilgisi eksik.");
        }
        if (isGroupCoordinator(editor, groupId)) {
            return;
        }
        if (!isCommitteeMemberForProject(editor, projectId)) {
            throw new AccessDeniedException(
                    "Story point düzenleme veya görüntüleme yetkiniz yok (komite veya grup koordinatörü).");
        }
    }

    public void requireGraderOnCommitteeForProject(User grader, Long projectId) {
        if (grader == null) {
            throw new IllegalArgumentException("Oturum açmanız gerekir.");
        }
        if (grader.getRole() == Role.ADMIN) {
            return;
        }
        if (projectId == null) {
            throw new IllegalArgumentException("Proje bilgisi eksik.");
        }
        if (!isCommitteeMemberForProject(grader, projectId)) {
            throw new AccessDeniedException(
                    "Bu proje veya şablon komitesinde değilsiniz; puanlama yapılamaz.");
        }
    }

    /**
     * Confirms the student group is (or was) assigned to the given project so cross-project IDs cannot be abused.
     */
    public void requireGroupAssignedToProject(Long groupId, Long projectId) {
        if (groupId == null || projectId == null) {
            throw new IllegalArgumentException("Grup veya proje bilgisi eksik.");
        }
        boolean viaAssignment =
                projectGroupAssignmentRepository
                        .findByGroupIdAndActiveTrue(groupId)
                        .map(a -> Objects.equals(a.getProject().getId(), projectId))
                        .orElse(false);
        if (viaAssignment) {
            return;
        }
        Project project = projectRepository
                .findById(projectId)
                .orElseThrow(() -> new AccessDeniedException("Proje bulunamadı."));
        if (Objects.equals(project.getGroupId(), groupId)) {
            return;
        }
        throw new AccessDeniedException("Bu grup bu projeye atanmıyor.");
    }
}
