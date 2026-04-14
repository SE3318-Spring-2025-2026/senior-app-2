package com.seniorapp.controller;

import com.seniorapp.dto.project.ProjectDtos.*;
import com.seniorapp.entity.User;
import com.seniorapp.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('COORDINATOR', 'ADMIN')")
    public ResponseEntity<IdResponse> createProject(
            @Valid @RequestBody CreateProjectRequest request,
            @AuthenticationPrincipal User principal,
            Authentication authentication
    ) {
        Long userId = principal != null ? principal.getId() : tryParseLong(authentication != null ? authentication.getName() : null);
        Long projectId = projectService.createProject(request, userId);
        return ResponseEntity.ok(new IdResponse("success", projectId));
    }

    @PostMapping("/{projectId}/group-assignment")
    @PreAuthorize("hasAnyRole('COORDINATOR', 'ADMIN')")
    public ResponseEntity<AssignmentResponse> assignGroup(
            @PathVariable Long projectId,
            @Valid @RequestBody AssignGroupRequest request,
            @AuthenticationPrincipal User principal,
            Authentication authentication
    ) {
        Long userId = principal != null ? principal.getId() : tryParseLong(authentication != null ? authentication.getName() : null);
        return ResponseEntity.ok(projectService.assignGroup(projectId, request.getGroupId(), userId));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('COORDINATOR', 'ADMIN')")
    public ResponseEntity<ProjectListResponse> listProjects(
            @RequestParam(required = false) String term,
            @RequestParam(required = false) Long templateId,
            @RequestParam(required = false) Long groupId
    ) {
        return ResponseEntity.ok(new ProjectListResponse("success", projectService.listProjects(term, templateId, groupId)));
    }

    @GetMapping("/{projectId}")
    @PreAuthorize("hasAnyRole('COORDINATOR', 'ADMIN')")
    public ResponseEntity<ProjectDetailResponse> getProjectDetail(@PathVariable Long projectId) {
        return ResponseEntity.ok(new ProjectDetailResponse("success", projectService.getProjectDetail(projectId)));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleValidation(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException e) {
        return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
    }

    private Long tryParseLong(String value) {
        if (value == null) return null;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
