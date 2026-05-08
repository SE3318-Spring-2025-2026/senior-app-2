package com.seniorapp.controller;

import com.seniorapp.entity.SprintAdvisorGrade;
import com.seniorapp.entity.SprintGradeType;
import com.seniorapp.service.SprintAdvisorGradeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Advisor API for submitting per-sprint Point A (Scrum) and Point B (Code Review) grades.
 */
@RestController
@RequestMapping("/api/advisor/sprint-grades")
@PreAuthorize("hasAnyRole('PROFESSOR','COORDINATOR','ADMIN')")
public class SprintAdvisorGradeController {

    private final SprintAdvisorGradeService gradeService;

    public SprintAdvisorGradeController(SprintAdvisorGradeService gradeService) {
        this.gradeService = gradeService;
    }

    /**
     * Submit or update a grade.
     * Body: { "groupId": 1, "gradeType": "SCRUM", "softGrade": "B", "comment": "..." }
     */
    @PutMapping("/sprint/{sprintId}")
    public ResponseEntity<SprintAdvisorGrade> upsert(
            @PathVariable Long sprintId,
            @RequestBody UpsertGradeRequest req,
            @RequestAttribute("userId") Long advisorUserId) {

        SprintAdvisorGrade saved = gradeService.upsert(
                sprintId, req.groupId(), advisorUserId,
                SprintGradeType.valueOf(req.gradeType()),
                req.softGrade(), req.comment());
        return ResponseEntity.ok(saved);
    }

    /** Get all grades for a group in a specific sprint. */
    @GetMapping("/sprint/{sprintId}/group/{groupId}")
    public ResponseEntity<List<SprintAdvisorGrade>> getBySprint(
            @PathVariable Long sprintId,
            @PathVariable Long groupId) {
        return ResponseEntity.ok(gradeService.getBySprintAndGroup(sprintId, groupId));
    }

    /** Get all grades for a group across all sprints in a project. */
    @GetMapping("/project/{projectId}/group/{groupId}")
    public ResponseEntity<List<SprintAdvisorGrade>> getByProject(
            @PathVariable Long projectId,
            @PathVariable Long groupId) {
        return ResponseEntity.ok(gradeService.getByProjectAndGroup(projectId, groupId));
    }

    record UpsertGradeRequest(Long groupId, String gradeType, String softGrade, String comment) {}
}
