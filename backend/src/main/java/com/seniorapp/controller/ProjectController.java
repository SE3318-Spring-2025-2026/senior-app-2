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
        return ResponseEntity.ok(projectService.assignGroup(projectId, request.getGroupId(), request.getCommitteeId(), userId));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('STUDENT', 'COORDINATOR', 'PROFESSOR', 'ADMIN')")
    public ResponseEntity<ProjectListResponse> listProjects(
            @RequestParam(required = false) String term,
            @RequestParam(required = false) Long templateId,
            @RequestParam(required = false) Long groupId,
            @AuthenticationPrincipal User principal,
            Authentication authentication
    ) {
        Long userId = principal != null ? principal.getId() : tryParseLong(authentication != null ? authentication.getName() : null);
        return ResponseEntity.ok(new ProjectListResponse("success", projectService.listProjects(term, templateId, groupId, userId)));
    }

    @GetMapping("/{projectId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'COORDINATOR', 'PROFESSOR', 'ADMIN')")
    public ResponseEntity<ProjectDetailResponse> getProjectDetail(
            @PathVariable Long projectId,
            @AuthenticationPrincipal User principal,
            Authentication authentication
    ) {
        Long userId = principal != null ? principal.getId() : tryParseLong(authentication != null ? authentication.getName() : null);
        return ResponseEntity.ok(new ProjectDetailResponse("success", projectService.getProjectDetail(projectId, userId)));
    }

    @GetMapping("/professors")
    @PreAuthorize("hasAnyRole('COORDINATOR', 'PROFESSOR', 'ADMIN')")
    public ResponseEntity<ProfessorListResponse> listProfessors() {
        return ResponseEntity.ok(new ProfessorListResponse("success", projectService.listProfessors()));
    }

    @GetMapping("/{projectId}/committees")
    @PreAuthorize("hasAnyRole('COORDINATOR', 'PROFESSOR', 'ADMIN')")
    public ResponseEntity<CommitteeListResponse> listCommittees(@PathVariable Long projectId) {
        return ResponseEntity.ok(new CommitteeListResponse("success", projectService.listCommittees(projectId)));
    }

    @GetMapping("/{projectId}/group-assignments")
    @PreAuthorize("hasAnyRole('COORDINATOR', 'PROFESSOR', 'ADMIN')")
    public ResponseEntity<GroupAssignmentListResponse> listProjectGroupAssignments(
            @PathVariable Long projectId,
            @AuthenticationPrincipal User principal,
            Authentication authentication
    ) {
        Long userId = principal != null ? principal.getId() : tryParseLong(authentication != null ? authentication.getName() : null);
        return ResponseEntity.ok(new GroupAssignmentListResponse("success", projectService.listProjectGroupAssignments(projectId, userId)));
    }

    @PostMapping("/{projectId}/committees")
    @PreAuthorize("hasAnyRole('COORDINATOR', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> createCommittee(
            @PathVariable Long projectId,
            @RequestBody(required = false) CreateCommitteeRequest request
    ) {
        String name = request != null ? request.getName() : null;
        CommitteeDto committee = projectService.createCommittee(projectId, name);
        return ResponseEntity.ok(Map.of("status", "success", "data", committee));
    }

    @PostMapping("/{projectId}/committees/{committeeId}/professors")
    @PreAuthorize("hasAnyRole('COORDINATOR', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> addProfessorToCommittee(
            @PathVariable Long projectId,
            @PathVariable Long committeeId,
            @Valid @RequestBody AddProfessorToCommitteeRequest request
    ) {
        CommitteeDto committee = projectService.addProfessorToCommittee(projectId, committeeId, request.getProfessorUserId());
        return ResponseEntity.ok(Map.of("status", "success", "data", committee));
    }

    @DeleteMapping("/{projectId}/committees/{committeeId}")
    @PreAuthorize("hasAnyRole('COORDINATOR', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteCommittee(
            @PathVariable Long projectId,
            @PathVariable Long committeeId
    ) {
        projectService.deleteCommittee(projectId, committeeId);
        return ResponseEntity.ok(Map.of("status", "success"));
    }

    @DeleteMapping("/{projectId}/committees/{committeeId}/professors/{professorUserId}")
    @PreAuthorize("hasAnyRole('COORDINATOR', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> removeProfessorFromCommittee(
            @PathVariable Long projectId,
            @PathVariable Long committeeId,
            @PathVariable Long professorUserId
    ) {
        CommitteeDto committee = projectService.removeProfessorFromCommittee(projectId, committeeId, professorUserId);
        return ResponseEntity.ok(Map.of("status", "success", "data", committee));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleValidation(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException e) {
        return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, String>> handleSecurity(SecurityException e) {
        return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
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
