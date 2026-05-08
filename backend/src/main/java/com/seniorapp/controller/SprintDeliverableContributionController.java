package com.seniorapp.controller;

import com.seniorapp.entity.SprintDeliverableContribution;
import com.seniorapp.service.SprintDeliverableContributionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Coordinator API for managing Sprint → Deliverable contribution percentages.
 *
 * <p>These values feed directly into the grading engine's scalar calculation:
 * each sprint's Point A/B grades are weighted by these percentages.
 */
@RestController
@RequestMapping("/api/coordinator/sprint-deliverable-contributions")
@PreAuthorize("hasAnyRole('COORDINATOR','ADMIN')")
public class SprintDeliverableContributionController {

    private final SprintDeliverableContributionService contributionService;

    public SprintDeliverableContributionController(SprintDeliverableContributionService contributionService) {
        this.contributionService = contributionService;
    }

    /**
     * Upsert a sprint → deliverable contribution.
     * Body: { "deliverableName": "PROPOSAL", "contributionPct": 70 }
     */
    @PutMapping("/sprint/{sprintId}")
    public ResponseEntity<SprintDeliverableContribution> upsert(
            @PathVariable Long sprintId,
            @RequestBody UpsertRequest req,
            @RequestAttribute("userId") Long coordinatorId) {

        SprintDeliverableContribution saved = contributionService.upsert(
                sprintId, req.deliverableName(), req.contributionPct(), coordinatorId);
        return ResponseEntity.ok(saved);
    }

    /** All contributions for a project. */
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<SprintDeliverableContribution>> byProject(
            @PathVariable Long projectId) {
        return ResponseEntity.ok(contributionService.getByProject(projectId));
    }

    /** All contributions for a single sprint. */
    @GetMapping("/sprint/{sprintId}")
    public ResponseEntity<List<SprintDeliverableContribution>> bySprint(
            @PathVariable Long sprintId) {
        return ResponseEntity.ok(contributionService.getBySprint(sprintId));
    }

    /** All sprint contributions that feed into a specific deliverable's scalar. */
    @GetMapping("/project/{projectId}/deliverable/{deliverableName}")
    public ResponseEntity<List<SprintDeliverableContribution>> byProjectAndDeliverable(
            @PathVariable Long projectId,
            @PathVariable String deliverableName) {
        return ResponseEntity.ok(contributionService.getByProjectAndDeliverable(projectId, deliverableName));
    }

    /** Delete a sprint → deliverable mapping. */
    @DeleteMapping("/sprint/{sprintId}/deliverable/{deliverableName}")
    public ResponseEntity<Map<String, String>> delete(
            @PathVariable Long sprintId,
            @PathVariable String deliverableName) {
        contributionService.delete(sprintId, deliverableName);
        return ResponseEntity.ok(Map.of("message", "Deleted successfully"));
    }

    record UpsertRequest(String deliverableName, Integer contributionPct) {}
}
