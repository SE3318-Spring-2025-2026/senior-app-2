package com.seniorapp.controller;

import com.seniorapp.dto.GradeResponse;
import com.seniorapp.dto.GradeSubmitRequest;
import com.seniorapp.entity.SubmissionGrade;
import com.seniorapp.entity.User;
import com.seniorapp.service.GradeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/deliverable-submissions")
public class GradeController {

    private final GradeService gradeService;

    public GradeController(GradeService gradeService) {
        this.gradeService = gradeService;
    }

    @PostMapping("/{submissionId}/grades")
    @PreAuthorize("hasAnyRole('COORDINATOR', 'PROFESSOR', 'ADMIN')")
    public ResponseEntity<GradeResponse> submitGrade(
            @PathVariable Long submissionId,
            @Valid @RequestBody GradeSubmitRequest request,
            @AuthenticationPrincipal User principal) {
        SubmissionGrade savedEntity = gradeService.saveGrade(submissionId, request, principal);
        return ResponseEntity.ok(new GradeResponse(savedEntity));
    }

    @GetMapping("/{submissionId}/grades")
    @PreAuthorize("hasAnyRole('COORDINATOR', 'PROFESSOR', 'ADMIN')")
    public ResponseEntity<List<GradeResponse>> getGrades(
            @PathVariable Long submissionId, @AuthenticationPrincipal User principal) {
        List<SubmissionGrade> grades = gradeService.getGradesForSubmission(submissionId, principal);
        List<GradeResponse> response = grades.stream()
                .map(GradeResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /** Öğrenci teslimi olmasa bile deliverable rubric notu (gerekirse boş teslim oluşturulur). */
    @PostMapping("/context/group/{groupId}/deliverable/{deliverableId}/grades")
    @PreAuthorize("hasAnyRole('COORDINATOR', 'PROFESSOR', 'ADMIN')")
    public ResponseEntity<GradeResponse> submitGradeByDeliverableContext(
            @PathVariable Long groupId,
            @PathVariable Long deliverableId,
            @Valid @RequestBody GradeSubmitRequest request,
            @AuthenticationPrincipal User principal) {
        SubmissionGrade saved = gradeService.saveGradeForDeliverableContext(groupId, deliverableId, request, principal);
        return ResponseEntity.ok(new GradeResponse(saved));
    }

    @GetMapping("/context/group/{groupId}/deliverable/{deliverableId}/grades")
    @PreAuthorize("hasAnyRole('COORDINATOR', 'PROFESSOR', 'ADMIN')")
    public ResponseEntity<List<GradeResponse>> getGradesByDeliverableContext(
            @PathVariable Long groupId,
            @PathVariable Long deliverableId,
            @AuthenticationPrincipal User principal) {
        List<SubmissionGrade> grades = gradeService.getGradesForDeliverableContext(groupId, deliverableId, principal);
        return ResponseEntity.ok(grades.stream().map(GradeResponse::new).collect(Collectors.toList()));
    }
}
