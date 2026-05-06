package com.seniorapp.service;

import com.seniorapp.dto.EvaluationGradeSubmitRequest;
import com.seniorapp.entity.EvaluationRubricGrade;
import com.seniorapp.entity.ProjectEvaluation;
import com.seniorapp.entity.ProjectEvaluationRubric;
import com.seniorapp.entity.User;
import com.seniorapp.repository.EvaluationRubricGradeRepository;
import com.seniorapp.repository.ProjectEvaluationRepository;
import com.seniorapp.repository.ProjectEvaluationRubricRepository;
import com.seniorapp.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EvaluationRubricGradeService {

    private final EvaluationRubricGradeRepository gradeRepository;
    private final ProjectEvaluationRubricRepository evaluationRubricRepository;
    private final ProjectEvaluationRepository projectEvaluationRepository;
    private final UserRepository userRepository;
    private final ProjectCommitteeGradeAccessService committeeGradeAccessService;

    public EvaluationRubricGradeService(
            EvaluationRubricGradeRepository gradeRepository,
            ProjectEvaluationRubricRepository evaluationRubricRepository,
            ProjectEvaluationRepository projectEvaluationRepository,
            UserRepository userRepository,
            ProjectCommitteeGradeAccessService committeeGradeAccessService) {
        this.gradeRepository = gradeRepository;
        this.evaluationRubricRepository = evaluationRubricRepository;
        this.projectEvaluationRepository = projectEvaluationRepository;
        this.userRepository = userRepository;
        this.committeeGradeAccessService = committeeGradeAccessService;
    }

    public EvaluationRubricGrade save(Long groupId, Long evaluationRubricId, EvaluationGradeSubmitRequest request, User principal) {
        if (principal == null) {
            throw new IllegalArgumentException("Oturum açmanız gerekir.");
        }
        ProjectEvaluationRubric rubric = evaluationRubricRepository
                .findByIdWithProjectChain(evaluationRubricId)
                .orElseThrow(() -> new IllegalArgumentException("Evaluation rubric bulunamadı: " + evaluationRubricId));

        User grader = userRepository.findById(principal.getId())
                .orElseThrow(() -> new IllegalArgumentException("Grader not found"));

        Long projectId = rubric.getEvaluation().getSprint().getProject().getId();
        committeeGradeAccessService.requireGraderOnCommitteeForProject(grader, projectId);
        committeeGradeAccessService.requireGroupAssignedToProject(groupId, projectId);

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
        User grader = userRepository.findById(viewer.getId())
                .orElseThrow(() -> new IllegalArgumentException("Kullanıcı bulunamadı."));
        ProjectEvaluation evaluation = projectEvaluationRepository
                .findByIdWithProjectChain(evaluationId)
                .orElseThrow(() -> new IllegalArgumentException("Değerlendirme bulunamadı: " + evaluationId));
        Long projectId = evaluation.getSprint().getProject().getId();
        committeeGradeAccessService.requireGraderOnCommitteeForProject(grader, projectId);
        committeeGradeAccessService.requireGroupAssignedToProject(groupId, projectId);
        return gradeRepository.findAllForGroupAndEvaluation(groupId, evaluationId);
    }
}
