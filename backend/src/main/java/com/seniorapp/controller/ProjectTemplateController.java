package com.seniorapp.controller;

import com.seniorapp.dto.projecttemplate.CreateProjectTemplateRequest;
import com.seniorapp.dto.projecttemplate.ProjectTemplateResponses.AddProfessorToCommitteeRequest;
import com.seniorapp.dto.projecttemplate.ProjectTemplateResponses.CreateCommitteeRequest;
import com.seniorapp.dto.projecttemplate.ProjectTemplateResponses.ProfessorListResponse;
import com.seniorapp.dto.projecttemplate.ProjectTemplateResponses.ProjectTemplateDetailResponse;
import com.seniorapp.dto.projecttemplate.ProjectTemplateResponses.ProjectTemplateListResponse;
import com.seniorapp.dto.projecttemplate.ProjectTemplateResponses.TemplateCommitteeListResponse;
import com.seniorapp.dto.projecttemplate.ProjectTemplateResponses.TemplateCommitteeDto;
import com.seniorapp.dto.projecttemplate.ProjectTemplateResponses.TemplateCreatedResponse;
import com.seniorapp.entity.User;
import com.seniorapp.service.ProjectTemplateService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/project-templates")
public class ProjectTemplateController {

    private final ProjectTemplateService projectTemplateService;

    public ProjectTemplateController(ProjectTemplateService projectTemplateService) {
        this.projectTemplateService = projectTemplateService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('COORDINATOR', 'ADMIN')")
    public ResponseEntity<TemplateCreatedResponse> createProjectTemplate(
            @Valid @RequestBody CreateProjectTemplateRequest request,
            @AuthenticationPrincipal User principal,
            Authentication authentication) {
        Long createdByUserId = principal != null ? principal.getId() : tryParseLong(authentication != null ? authentication.getName() : null);
        Long templateId = projectTemplateService.createTemplate(request, createdByUserId);
        return ResponseEntity.ok(
                new TemplateCreatedResponse("success", "Project template created successfully.", templateId)
        );
    }

    private Long tryParseLong(String value) {
        if (value == null) return null;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('COORDINATOR', 'PROFESSOR', 'ADMIN')")
    public ResponseEntity<ProjectTemplateListResponse> listProjectTemplates(
            @AuthenticationPrincipal User principal,
            Authentication authentication
    ) {
        Long requesterUserId = principal != null ? principal.getId() : tryParseLong(authentication != null ? authentication.getName() : null);
        boolean isProfessor = isProfessor(authentication);
        return ResponseEntity.ok(
                new ProjectTemplateListResponse("success", projectTemplateService.listTemplates(requesterUserId, isProfessor))
        );
    }

    @GetMapping("/{templateId}")
    @PreAuthorize("hasAnyRole('COORDINATOR', 'PROFESSOR', 'ADMIN')")
    public ResponseEntity<ProjectTemplateDetailResponse> getProjectTemplate(
            @PathVariable Long templateId,
            @AuthenticationPrincipal User principal,
            Authentication authentication
    ) {
        Long requesterUserId = principal != null ? principal.getId() : tryParseLong(authentication != null ? authentication.getName() : null);
        boolean isProfessor = isProfessor(authentication);
        return ResponseEntity.ok(
                new ProjectTemplateDetailResponse("success", projectTemplateService.getTemplate(templateId, requesterUserId, isProfessor))
        );
    }

    @GetMapping("/professors")
    @PreAuthorize("hasAnyRole('COORDINATOR', 'PROFESSOR', 'ADMIN')")
    public ResponseEntity<ProfessorListResponse> listProfessors() {
        return ResponseEntity.ok(new ProfessorListResponse("success", projectTemplateService.listProfessors()));
    }

    @GetMapping("/{templateId}/committees")
    @PreAuthorize("hasAnyRole('COORDINATOR', 'PROFESSOR', 'ADMIN')")
    public ResponseEntity<TemplateCommitteeListResponse> listTemplateCommittees(
            @PathVariable Long templateId,
            @AuthenticationPrincipal User principal,
            Authentication authentication
    ) {
        Long requesterUserId = principal != null ? principal.getId() : tryParseLong(authentication != null ? authentication.getName() : null);
        boolean isProfessor = isProfessor(authentication);
        return ResponseEntity.ok(new TemplateCommitteeListResponse(
                "success",
                projectTemplateService.listTemplateCommittees(templateId, requesterUserId, isProfessor)
        ));
    }

    private boolean isProfessor(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_PROFESSOR"::equals);
    }

    @PostMapping("/{templateId}/committees")
    @PreAuthorize("hasAnyRole('COORDINATOR', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> createTemplateCommittee(
            @PathVariable Long templateId,
            @RequestBody(required = false) CreateCommitteeRequest request
    ) {
        String name = request != null ? request.getName() : null;
        TemplateCommitteeDto committee = projectTemplateService.createTemplateCommittee(templateId, name);
        return ResponseEntity.ok(Map.of("status", "success", "data", committee));
    }

    @PostMapping("/{templateId}/committees/{committeeId}/professors")
    @PreAuthorize("hasAnyRole('COORDINATOR', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> addProfessorToCommittee(
            @PathVariable Long templateId,
            @PathVariable Long committeeId,
            @RequestBody AddProfessorToCommitteeRequest request
    ) {
        if (request == null || request.getProfessorUserId() == null) {
            throw new IllegalArgumentException("professorUserId is required.");
        }
        TemplateCommitteeDto committee = projectTemplateService.addProfessorToTemplateCommittee(
                templateId,
                committeeId,
                request.getProfessorUserId()
        );
        return ResponseEntity.ok(Map.of("status", "success", "data", committee));
    }

    @DeleteMapping("/{templateId}/committees/{committeeId}")
    @PreAuthorize("hasAnyRole('COORDINATOR', 'ADMIN')")
    public ResponseEntity<Map<String, String>> deleteTemplateCommittee(
            @PathVariable Long templateId,
            @PathVariable Long committeeId
    ) {
        projectTemplateService.deleteTemplateCommittee(templateId, committeeId);
        return ResponseEntity.ok(Map.of("status", "success"));
    }

    @DeleteMapping("/{templateId}/committees/{committeeId}/professors/{professorUserId}")
    @PreAuthorize("hasAnyRole('COORDINATOR', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> removeProfessorFromCommittee(
            @PathVariable Long templateId,
            @PathVariable Long committeeId,
            @PathVariable Long professorUserId
    ) {
        TemplateCommitteeDto committee = projectTemplateService.removeProfessorFromTemplateCommittee(
                templateId,
                committeeId,
                professorUserId
        );
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
}
