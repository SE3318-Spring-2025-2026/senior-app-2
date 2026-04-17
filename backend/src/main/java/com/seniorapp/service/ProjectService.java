package com.seniorapp.service;

import org.springframework.stereotype.Service;

import com.seniorapp.dto.ProjectTemplateResponse;
import com.seniorapp.dto.ProjectTemplateResponse.ProjectTemplateItem;
import com.seniorapp.entity.Project;
import com.seniorapp.repository.ProjectRepository;

import jakarta.transaction.Transactional;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service responsible for managing project templates that students can choose from when creating a new project.
 */
@Service
public class ProjectService {
    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);
    
    private ProjectRepository projectRepository;
    /**
     * Constructor for the ProjectService, requires Spring to connect a ProjectRepository
     * @param projectRepository 
     */
    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;

    }

    /**
     * Returns the list of project templates that students can choose from when creating a new project.
     * @return a List<Project> containing all projects
     */
    @Transactional
    public ProjectTemplateResponse getProjectTemplates() {
        List<Project> projects = projectRepository.findAll();

        List<ProjectTemplateItem> items = projects.stream()
                .map(project -> new ProjectTemplateItem(
                        project.getProjectId(),
                        project.getName(),
                        project.getSprintCount(),
                        List.of(project.getDeliverables())
                ))
                .toList();

        ProjectTemplateResponse response = new ProjectTemplateResponse(items);
        log.debug("Fetched {} project templates from database.", projects.size());
        return response;
    }
}
