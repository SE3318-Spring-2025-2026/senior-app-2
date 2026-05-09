package com.seniorapp.service;

import com.seniorapp.dto.GradeSubmitRequest;
import com.seniorapp.entity.DeliverableSubmission;
import com.seniorapp.entity.GroupInviteStatus;
import com.seniorapp.entity.ProjectDeliverable;
import com.seniorapp.entity.Role;
import com.seniorapp.entity.SubmissionGrade;
import com.seniorapp.entity.User;
import com.seniorapp.repository.DeliverableSubmissionRepository;
import com.seniorapp.repository.ProjectDeliverableRepository;
import com.seniorapp.repository.SubmissionGradeRepository;
import com.seniorapp.repository.UserGroupMemberRepository;
import com.seniorapp.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class GradeService {
    private final SubmissionGradeRepository gradeRepository;
    private final DeliverableSubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final DeliverableSubmissionService deliverableSubmissionService;
    private final ProjectCommitteeGradeAccessService committeeGradeAccessService;
    private final ProjectDeliverableRepository projectDeliverableRepository;
    private final UserGroupMemberRepository userGroupMemberRepository;

    public GradeService(
            SubmissionGradeRepository gradeRepository,
            DeliverableSubmissionRepository submissionRepository,
            UserRepository userRepository,
            DeliverableSubmissionService deliverableSubmissionService,
            ProjectCommitteeGradeAccessService committeeGradeAccessService,
            ProjectDeliverableRepository projectDeliverableRepository,
            UserGroupMemberRepository userGroupMemberRepository) {
        this.gradeRepository = gradeRepository;
        this.submissionRepository = submissionRepository;
        this.userRepository = userRepository;
        this.deliverableSubmissionService = deliverableSubmissionService;
        this.committeeGradeAccessService = committeeGradeAccessService;
        this.projectDeliverableRepository = projectDeliverableRepository;
        this.userGroupMemberRepository = userGroupMemberRepository;
    }

    /**
     * Creates or updates the grade row for (submission, authenticated grader, rubric).
     */
    public SubmissionGrade saveGrade(Long submissionId, GradeSubmitRequest request, User graderPrincipal) {
        if (graderPrincipal == null) {
            throw new IllegalArgumentException("Oturum açmanız gerekir.");
        }
        Long safeGraderId = Objects.requireNonNull(graderPrincipal.getId(), "grader id is required");

        DeliverableSubmission submission = submissionRepository
                .findByIdWithProjectChain(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found with ID: " + submissionId));

        User grader = userRepository.findById(safeGraderId)
                .orElseThrow(() -> new IllegalArgumentException("Grader not found with ID: " + safeGraderId));

        Long projectId = Objects.requireNonNull(submission.getDeliverable().getSprint().getProject().getId(), "projectId is required");
        committeeGradeAccessService.requireGraderOnCommitteeForProject(grader, projectId);
        if (grader.getRole() != Role.ADMIN && grader.getRole() != Role.COORDINATOR) {
            committeeGradeAccessService.requireGroupAssignedToProject(submission.getGroupId(), projectId);
        }

        Optional<SubmissionGrade> existing = gradeRepository.findBySubmission_IdAndGrader_IdAndRubricId(
                submissionId, grader.getId(), request.getRubricId());

        SubmissionGrade grade = existing.orElseGet(() -> {
            SubmissionGrade g = new SubmissionGrade();
            g.setSubmission(submission);
            g.setGrader(grader);
            g.setRubricId(request.getRubricId());
            return g;
        });

        grade.setGrade(request.getGrade());
        grade.setComment(request.getComment());

        return gradeRepository.save(grade);
    }

    /**
     * Teslim yoksa boş kayıt açıp aynı deliverable rubric notunu yazar.
     */
    public SubmissionGrade saveGradeForDeliverableContext(Long groupId, Long deliverableId, GradeSubmitRequest request, User graderPrincipal) {
        if (graderPrincipal == null) {
            throw new IllegalArgumentException("Oturum açmanız gerekir.");
        }
        Long safeGraderId = Objects.requireNonNull(graderPrincipal.getId(), "grader id is required");
        User grader = userRepository.findById(safeGraderId)
                .orElseThrow(() -> new IllegalArgumentException("Grader not found with ID: " + safeGraderId));

        ProjectDeliverable deliverable = projectDeliverableRepository
                .findByIdWithProjectChain(deliverableId)
                .orElseThrow(() -> new IllegalArgumentException("Deliverable bulunamadı: " + deliverableId));
        Long projectId = Objects.requireNonNull(deliverable.getSprint().getProject().getId(), "projectId is required");
        committeeGradeAccessService.requireGraderOnCommitteeForProject(grader, projectId);
        if (grader.getRole() != Role.ADMIN && grader.getRole() != Role.COORDINATOR) {
            committeeGradeAccessService.requireGroupAssignedToProject(groupId, projectId);
        }

        DeliverableSubmission submission = deliverableSubmissionService.ensureGradingPlaceholder(
                deliverableId, groupId, graderPrincipal.getId());
        return saveGrade(submission.getId(), request, graderPrincipal);
    }

    public List<SubmissionGrade> getGradesForSubmission(Long submissionId, User viewer) {
        if (viewer == null) {
            throw new IllegalArgumentException("Oturum açmanız gerekir.");
        }
        Long safeViewerId = Objects.requireNonNull(viewer.getId(), "viewer id is required");
        DeliverableSubmission submission = submissionRepository
                .findByIdWithProjectChain(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found with ID: " + submissionId));
        User grader = userRepository.findById(safeViewerId)
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı."));
        Long projectId = Objects.requireNonNull(submission.getDeliverable().getSprint().getProject().getId(), "projectId is required");
        Long safeSubmissionGroupId = Objects.requireNonNull(submission.getGroupId(), "submission groupId is required");
        if (grader.getRole() == Role.STUDENT) {
            boolean inGroup = userGroupMemberRepository.existsByGroupIdAndUserIdAndStatus(
                    safeSubmissionGroupId, grader.getId(), GroupInviteStatus.ACCEPTED);
            if (!inGroup) {
                throw new org.springframework.security.access.AccessDeniedException("Bu notları görüntüleme yetkiniz yok.");
            }
        } else {
            committeeGradeAccessService.requireGraderOnCommitteeForProject(grader, projectId);
            if (grader.getRole() != Role.ADMIN && grader.getRole() != Role.COORDINATOR) {
                committeeGradeAccessService.requireGroupAssignedToProject(safeSubmissionGroupId, projectId);
            }
        }
        return gradeRepository.findAllForSubmission(submissionId);
    }

    public List<SubmissionGrade> getGradesForDeliverableContext(Long groupId, Long deliverableId, User viewer) {
        if (viewer == null) {
            throw new IllegalArgumentException("Oturum açmanız gerekir.");
        }
        Long safeViewerId = Objects.requireNonNull(viewer.getId(), "viewer id is required");
        User grader = userRepository.findById(safeViewerId)
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı."));
        ProjectDeliverable deliverable = projectDeliverableRepository
                .findByIdWithProjectChain(deliverableId)
                .orElseThrow(() -> new IllegalArgumentException("Deliverable bulunamadı: " + deliverableId));
        Long projectId = Objects.requireNonNull(deliverable.getSprint().getProject().getId(), "projectId is required");
        Long safeGroupId = Objects.requireNonNull(groupId, "groupId is required");
        if (grader.getRole() == Role.STUDENT) {
            boolean inGroup = userGroupMemberRepository.existsByGroupIdAndUserIdAndStatus(
                    safeGroupId, grader.getId(), GroupInviteStatus.ACCEPTED);
            if (!inGroup) {
                throw new org.springframework.security.access.AccessDeniedException("Bu notları görüntüleme yetkiniz yok.");
            }
        } else {
            committeeGradeAccessService.requireGraderOnCommitteeForProject(grader, projectId);
            if (grader.getRole() != Role.ADMIN && grader.getRole() != Role.COORDINATOR) {
                committeeGradeAccessService.requireGroupAssignedToProject(safeGroupId, projectId);
            }
        }
        return submissionRepository
                .findByDeliverableIdAndGroupId(deliverableId, safeGroupId)
                .map(s -> gradeRepository.findAllForSubmission(s.getId()))
                .orElseGet(List::of);
    }
}