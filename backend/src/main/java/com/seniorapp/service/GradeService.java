package com.seniorapp.service;

import com.seniorapp.dto.GradeSubmitRequest;
import com.seniorapp.entity.DeliverableSubmission;
import com.seniorapp.entity.ProjectDeliverable;
import com.seniorapp.entity.SubmissionGrade;
import com.seniorapp.entity.User;
import com.seniorapp.repository.DeliverableSubmissionRepository;
import com.seniorapp.repository.ProjectDeliverableRepository;
import com.seniorapp.repository.SubmissionGradeRepository;
import com.seniorapp.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class GradeService {
    private final SubmissionGradeRepository gradeRepository;
    private final DeliverableSubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    private final DeliverableSubmissionService deliverableSubmissionService;
    private final ProjectCommitteeGradeAccessService committeeGradeAccessService;
    private final ProjectDeliverableRepository projectDeliverableRepository;

    public GradeService(
            SubmissionGradeRepository gradeRepository,
            DeliverableSubmissionRepository submissionRepository,
            UserRepository userRepository,
            DeliverableSubmissionService deliverableSubmissionService,
            ProjectCommitteeGradeAccessService committeeGradeAccessService,
            ProjectDeliverableRepository projectDeliverableRepository) {
        this.gradeRepository = gradeRepository;
        this.submissionRepository = submissionRepository;
        this.userRepository = userRepository;
        this.deliverableSubmissionService = deliverableSubmissionService;
        this.committeeGradeAccessService = committeeGradeAccessService;
        this.projectDeliverableRepository = projectDeliverableRepository;
    }

    /**
     * Creates or updates the grade row for (submission, authenticated grader, rubric).
     */
    public SubmissionGrade saveGrade(Long submissionId, GradeSubmitRequest request, User graderPrincipal) {
        if (graderPrincipal == null) {
            throw new IllegalArgumentException("Oturum açmanız gerekir.");
        }

        DeliverableSubmission submission = submissionRepository
                .findByIdWithProjectChain(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found with ID: " + submissionId));

        User grader = userRepository.findById(graderPrincipal.getId())
                .orElseThrow(() -> new IllegalArgumentException("Grader not found with ID: " + graderPrincipal.getId()));

        Long projectId = submission.getDeliverable().getSprint().getProject().getId();
        committeeGradeAccessService.requireGraderOnCommitteeForProject(grader, projectId);
        committeeGradeAccessService.requireGroupAssignedToProject(submission.getGroupId(), projectId);

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
        User grader = userRepository.findById(graderPrincipal.getId())
                .orElseThrow(() -> new IllegalArgumentException("Grader not found with ID: " + graderPrincipal.getId()));

        ProjectDeliverable deliverable = projectDeliverableRepository
                .findByIdWithProjectChain(deliverableId)
                .orElseThrow(() -> new IllegalArgumentException("Deliverable bulunamadı: " + deliverableId));
        Long projectId = deliverable.getSprint().getProject().getId();
        committeeGradeAccessService.requireGraderOnCommitteeForProject(grader, projectId);
        committeeGradeAccessService.requireGroupAssignedToProject(groupId, projectId);

        DeliverableSubmission submission = deliverableSubmissionService.ensureGradingPlaceholder(
                deliverableId, groupId, graderPrincipal.getId());
        return saveGrade(submission.getId(), request, graderPrincipal);
    }

    public List<SubmissionGrade> getGradesForSubmission(Long submissionId, User viewer) {
        if (viewer == null) {
            throw new IllegalArgumentException("Oturum açmanız gerekir.");
        }
        DeliverableSubmission submission = submissionRepository
                .findByIdWithProjectChain(submissionId)
                .orElseThrow(() -> new IllegalArgumentException("Submission not found with ID: " + submissionId));
        User grader = userRepository.findById(viewer.getId())
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı."));
        Long projectId = submission.getDeliverable().getSprint().getProject().getId();
        committeeGradeAccessService.requireGraderOnCommitteeForProject(grader, projectId);
        committeeGradeAccessService.requireGroupAssignedToProject(submission.getGroupId(), projectId);
        return gradeRepository.findAllForSubmission(submissionId);
    }

    public List<SubmissionGrade> getGradesForDeliverableContext(Long groupId, Long deliverableId, User viewer) {
        if (viewer == null) {
            throw new IllegalArgumentException("Oturum açmanız gerekir.");
        }
        User grader = userRepository.findById(viewer.getId())
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı."));
        ProjectDeliverable deliverable = projectDeliverableRepository
                .findByIdWithProjectChain(deliverableId)
                .orElseThrow(() -> new IllegalArgumentException("Deliverable bulunamadı: " + deliverableId));
        Long projectId = deliverable.getSprint().getProject().getId();
        committeeGradeAccessService.requireGraderOnCommitteeForProject(grader, projectId);
        committeeGradeAccessService.requireGroupAssignedToProject(groupId, projectId);
        return submissionRepository
                .findByDeliverableIdAndGroupId(deliverableId, groupId)
                .map(s -> gradeRepository.findAllForSubmission(s.getId()))
                .orElseGet(List::of);
    }
}
