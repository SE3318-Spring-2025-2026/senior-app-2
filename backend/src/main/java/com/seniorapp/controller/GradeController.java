package com.seniorapp.controller;

import com.seniorapp.dto.GradeResponse;
import com.seniorapp.dto.GradeSubmitRequest;
import com.seniorapp.entity.SubmissionGrade;
import com.seniorapp.service.GradeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
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
            @Valid @RequestBody GradeSubmitRequest request) {
        SubmissionGrade savedEntity = gradeService.saveGrade(submissionId, request);
        return ResponseEntity.ok(new GradeResponse(savedEntity));
    }

    @GetMapping("/{submissionId}/grades")
    @PreAuthorize("hasAnyRole('STUDENT', 'COORDINATOR', 'PROFESSOR', 'ADMIN')")
    public ResponseEntity<List<GradeResponse>> getGrades(@PathVariable Long submissionId) {
        List<SubmissionGrade> grades = gradeService.getGradesForSubmission(submissionId);
        List<GradeResponse> response = grades.stream()
                .map(GradeResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, String>> handleSecurity(SecurityException e) {
        return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
    }
}
