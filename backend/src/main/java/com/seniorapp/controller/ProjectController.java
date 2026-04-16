package com.seniorapp.controller;

import com.seniorapp.dto.DeliverableStatusDto;
import com.seniorapp.dto.ProjectInspectionDto;
import com.seniorapp.service.ProjectService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public ResponseEntity<Page<ProjectInspectionDto>> getProjects(
            @RequestParam(required = false) String term,
            @RequestParam(required = false) Long committeeId,
            @RequestParam(required = false) Long advisorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Page<ProjectInspectionDto> result = projectService.getProjectsByFilter(term, committeeId, advisorId, PageRequest.of(page, size));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{projectId}/deliverables/status")
    public ResponseEntity<List<DeliverableStatusDto>> getDeliverableStatuses(@PathVariable Long projectId) {
        List<DeliverableStatusDto> statuses = projectService.getProjectDeliverableStatuses(projectId);
        return ResponseEntity.ok(statuses);
    }
}
