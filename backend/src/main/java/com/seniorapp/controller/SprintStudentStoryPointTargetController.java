package com.seniorapp.controller;

import com.seniorapp.entity.SprintStudentStoryPointTarget;
import com.seniorapp.service.SprintStudentStoryPointTargetService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Coordinator API for managing per-student per-sprint story point targets.
 */
@RestController
@RequestMapping("/api/coordinator/sprint-sp-targets")
@PreAuthorize("hasAnyRole('COORDINATOR','ADMIN')")
public class SprintStudentStoryPointTargetController {

    private final SprintStudentStoryPointTargetService targetService;

    public SprintStudentStoryPointTargetController(SprintStudentStoryPointTargetService targetService) {
        this.targetService = targetService;
    }

    /**
     * Set or update a story-point target.
     * Body: { "studentUserId": 5, "targetPoints": 5 }
     */
    @PutMapping("/sprint/{sprintId}")
    public ResponseEntity<SprintStudentStoryPointTarget> upsert(
            @PathVariable Long sprintId,
            @RequestBody UpsertTargetRequest req,
            @RequestAttribute("userId") Long coordinatorId) {

        return ResponseEntity.ok(
                targetService.upsert(sprintId, req.studentUserId(), req.targetPoints(), coordinatorId));
    }

    /** Get all targets for a sprint (all students). */
    @GetMapping("/sprint/{sprintId}")
    public ResponseEntity<List<SprintStudentStoryPointTarget>> getBySprint(@PathVariable Long sprintId) {
        return ResponseEntity.ok(targetService.getBySprintId(sprintId));
    }

    /** Get all targets for a student across a project's sprints. */
    @GetMapping("/project/{projectId}/student/{studentUserId}")
    public ResponseEntity<List<SprintStudentStoryPointTarget>> getByProjectAndStudent(
            @PathVariable Long projectId,
            @PathVariable Long studentUserId) {
        return ResponseEntity.ok(targetService.getByProjectAndStudent(projectId, studentUserId));
    }

    /** Get target for a specific student in a sprint. */
    @GetMapping("/sprint/{sprintId}/student/{studentUserId}")
    public ResponseEntity<SprintStudentStoryPointTarget> getTarget(
            @PathVariable Long sprintId,
            @PathVariable Long studentUserId) {
        return ResponseEntity.ok(targetService.getTarget(sprintId, studentUserId));
    }

    record UpsertTargetRequest(Long studentUserId, Integer targetPoints) {}
}
