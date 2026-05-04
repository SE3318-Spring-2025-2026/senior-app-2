package com.seniorapp.controller;

import com.seniorapp.dto.EvaluationGradeResponse;
import com.seniorapp.dto.EvaluationGradeSubmitRequest;
import com.seniorapp.entity.EvaluationRubricGrade;
import com.seniorapp.entity.User;
import com.seniorapp.service.EvaluationRubricGradeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/evaluation-grades")
public class EvaluationRubricGradeController {

    private final EvaluationRubricGradeService evaluationRubricGradeService;

    public EvaluationRubricGradeController(EvaluationRubricGradeService evaluationRubricGradeService) {
        this.evaluationRubricGradeService = evaluationRubricGradeService;
    }

    @PostMapping("/group/{groupId}/rubrics/{evaluationRubricId}")
    @PreAuthorize("hasAnyRole('COORDINATOR', 'PROFESSOR', 'ADMIN')")
    public ResponseEntity<EvaluationGradeResponse> submit(
            @PathVariable Long groupId,
            @PathVariable Long evaluationRubricId,
            @Valid @RequestBody EvaluationGradeSubmitRequest request,
            @AuthenticationPrincipal User principal) {
        EvaluationRubricGrade saved = evaluationRubricGradeService.save(groupId, evaluationRubricId, request, principal);
        return ResponseEntity.ok(new EvaluationGradeResponse(saved));
    }

    @GetMapping("/group/{groupId}/evaluations/{evaluationId}")
    @PreAuthorize("hasAnyRole('COORDINATOR', 'PROFESSOR', 'ADMIN')")
    public ResponseEntity<List<EvaluationGradeResponse>> listForEvaluation(
            @PathVariable Long groupId,
            @PathVariable Long evaluationId) {
        List<EvaluationRubricGrade> list = evaluationRubricGradeService.listForGroupAndEvaluation(groupId, evaluationId);
        return ResponseEntity.ok(list.stream().map(EvaluationGradeResponse::new).collect(Collectors.toList()));
    }
}
