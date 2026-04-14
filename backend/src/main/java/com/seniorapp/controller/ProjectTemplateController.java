package com.seniorapp.controller;

import com.seniorapp.dto.projecttemplate.CreateProjectTemplateRequest;
import com.seniorapp.dto.projecttemplate.ProjectTemplateResponses.ProjectTemplateDetailResponse;
import com.seniorapp.dto.projecttemplate.ProjectTemplateResponses.ProjectTemplateListResponse;
import com.seniorapp.dto.projecttemplate.ProjectTemplateResponses.TemplateCreatedResponse;
import com.seniorapp.entity.User;
import com.seniorapp.service.ProjectTemplateService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.Authentication;
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
    @PreAuthorize("hasAnyRole('COORDINATOR', 'ADMIN')")
    public ResponseEntity<ProjectTemplateListResponse> listProjectTemplates() {
        return ResponseEntity.ok(
                new ProjectTemplateListResponse("success", projectTemplateService.listTemplates())
        );
    }

    @GetMapping("/{templateId}")
    @PreAuthorize("hasAnyRole('COORDINATOR', 'ADMIN')")
    public ResponseEntity<ProjectTemplateDetailResponse> getProjectTemplate(@PathVariable Long templateId) {
        return ResponseEntity.ok(
                new ProjectTemplateDetailResponse("success", projectTemplateService.getTemplate(templateId))
        );
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
