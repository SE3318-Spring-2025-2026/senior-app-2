package com.seniorapp.service;

import com.seniorapp.dto.EvaluationGradeSubmitRequest;
import com.seniorapp.entity.EvaluationRubricGrade;
import com.seniorapp.entity.GroupInviteStatus;
import com.seniorapp.entity.ProjectEvaluation;
import com.seniorapp.entity.ProjectEvaluationRubric;
import com.seniorapp.entity.Role;
import com.seniorapp.entity.User;
import com.seniorapp.repository.EvaluationRubricGradeRepository;
import com.seniorapp.repository.ProjectEvaluationRepository;
import com.seniorapp.repository.ProjectEvaluationRubricRepository;
import com.seniorapp.repository.UserGroupMemberRepository;
import com.seniorapp.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class EvaluationRubricGradeService {

    private final EvaluationRubricGradeRepository gradeRepository;
    private final ProjectEvaluationRubricRepository evaluationRubricRepository;
    private final ProjectEvaluationRepository projectEvaluationRepository;
    private final UserRepository userRepository;
    private final ProjectCommitteeGradeAccessService committeeGradeAccessService;
    private final UserGroupMemberRepository userGroupMemberRepository;

    public EvaluationRubricGradeService(
            EvaluationRubricGradeRepository gradeRepository,
            ProjectEvaluationRubricRepository evaluationRubricRepository,
            ProjectEvaluationRepository projectEvaluationRepository,
            UserRepository userRepository,
            ProjectCommitteeGradeAccessService committeeGradeAccessService,
            UserGroupMemberRepository userGroupMemberRepository) {
        this.gradeRepository = gradeRepository;
        this.evaluationRubricRepository = evaluationRubricRepository;
        this.projectEvaluationRepository = projectEvaluationRepository;
        this.userRepository = userRepository;
        this.committeeGradeAccessService = committeeGradeAccessService;
        this.userGroupMemberRepository = userGroupMemberRepository;
    }

    public EvaluationRubricGrade save(Long groupId, Long evaluationRubricId, EvaluationGradeSubmitRequest request, User principal) {
        if (principal == null) {
            throw new IllegalArgumentException("Oturum açmanız gerekir.");
        }
        Long safePrincipalId = Objects.requireNonNull(principal.getId(), "principal id is required");
        ProjectEvaluationRubric rubric = evaluationRubricRepository
                .findByIdWithProjectChain(evaluationRubricId)
                .orElseThrow(() -> new IllegalArgumentException("Evaluation rubric bulunamadı: " + evaluationRubricId));

        User grader = userRepository.findById(safePrincipalId)
                .orElseThrow(() -> new IllegalArgumentException("Grader not found"));

        Long projectId = rubric.getEvaluation().getSprint().getProject().getId();
        committeeGradeAccessService.requireGraderOnCommitteeForProject(grader, projectId);
        if (grader.getRole() != Role.ADMIN && grader.getRole() != Role.COORDINATOR) {
            committeeGradeAccessService.requireGroupAssignedToProject(groupId, projectId);
        }

        Optional<EvaluationRubricGrade> existing = gradeRepository.findByGroupIdAndGrader_IdAndEvaluationRubric_Id(
                groupId, grader.getId(), evaluationRubricId);

        EvaluationRubricGrade row = existing.orElseGet(() -> {
            EvaluationRubricGrade g = new EvaluationRubricGrade();
            g.setGroupId(groupId);
            g.setEvaluationRubric(rubric);
            g.setGrader(grader);
            return g;
        });

        row.setGrade(request.getGrade());
        row.setComment(request.getComment());

        return gradeRepository.save(row);
    }

    public List<EvaluationRubricGrade> listForGroupAndEvaluation(Long groupId, Long evaluationId, User viewer) {
        if (viewer == null) {
            throw new IllegalArgumentException("Oturum açmanız gerekir.");
        }
        Long safeViewerId = Objects.requireNonNull(viewer.getId(), "viewer id is required");
        Long safeGroupId = Objects.requireNonNull(groupId, "groupId is required");
        User grader = userRepository.findById(safeViewerId)
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı."));
        ProjectEvaluation evaluation = projectEvaluationRepository
                .findByIdWithProjectChain(evaluationId)
                .orElseThrow(() -> new IllegalArgumentException("Değerlendirme bulunamadı: " + evaluationId));
        Long projectId = Objects.requireNonNull(evaluation.getSprint().getProject().getId(), "projectId is required");
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
        return gradeRepository.findAllForGroupAndEvaluation(safeGroupId, evaluationId);
    }
}
