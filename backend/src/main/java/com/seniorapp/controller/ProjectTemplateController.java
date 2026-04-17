package com.seniorapp.controller;

import com.seniorapp.dto.ProjectTemplateResponse;
import com.seniorapp.service.ProjectTemplateService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/project-templates")
@CrossOrigin(origins = "*")
public class ProjectTemplateController {
    private final ProjectTemplateService projectTemplateService;

    public ProjectTemplateController(ProjectTemplateService projectTemplateService) {
        this.projectTemplateService = projectTemplateService;
    }

    @GetMapping
    public List<ProjectTemplateResponse> getAvailableTemplates() {
        return projectTemplateService.getAvailableTemplates();
    }
}
