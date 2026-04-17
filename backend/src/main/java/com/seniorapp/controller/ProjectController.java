package com.seniorapp.controller;

import com.seniorapp.dto.ProjectTemplateResponse;
import com.seniorapp.service.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private static final Logger log = LoggerFactory.getLogger(ProjectController.class);

    private final ProjectService projectService;

    /**
     * Constructor for the ProjectController, requires Spring to connect a ProjectService
     */
    public ProjectController(ProjectService projectService) { this.projectService = projectService; }

    /**
     * Returns the list of project templates that students can choose from when creating a new project.
     */
    @GetMapping
    public ResponseEntity<ProjectTemplateResponse> getProjectTemplates() {
        ProjectTemplateResponse body = projectService.getProjectTemplates();
        log.debug("Project templates: count={}", body.getProjects().size());

        return ResponseEntity.ok(body);
    }
}
