package com.seniorapp.controller;

import com.seniorapp.dto.project.StoryPointDtos.SaveStoryPointsRequest;
import com.seniorapp.dto.project.StoryPointDtos.StoryPointsListResponse;
import com.seniorapp.entity.User;
import com.seniorapp.service.ProjectStoryPointService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects/{projectId}/groups/{groupId}/story-points")
public class ProjectGroupStoryPointController {

    private final ProjectStoryPointService projectStoryPointService;

    public ProjectGroupStoryPointController(ProjectStoryPointService projectStoryPointService) {
        this.projectStoryPointService = projectStoryPointService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('COORDINATOR', 'PROFESSOR', 'ADMIN')")
    public ResponseEntity<StoryPointsListResponse> list(
            @PathVariable Long projectId,
            @PathVariable Long groupId,
            @AuthenticationPrincipal User principal) {
        return ResponseEntity.ok(projectStoryPointService.list(projectId, groupId, principal));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('COORDINATOR', 'PROFESSOR', 'ADMIN')")
    public ResponseEntity<StoryPointsListResponse> save(
            @PathVariable Long projectId,
            @PathVariable Long groupId,
            @Valid @RequestBody SaveStoryPointsRequest body,
            @AuthenticationPrincipal User principal) {
        return ResponseEntity.ok(projectStoryPointService.save(projectId, groupId, principal, body));
    }
}
