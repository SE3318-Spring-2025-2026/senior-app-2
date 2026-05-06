package com.seniorapp.controller;

import com.seniorapp.dto.comparison.ComparisonDtos.*;
import com.seniorapp.service.ComparisonService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comparison")
public class ComparisonController {

    private final ComparisonService comparisonService;

    public ComparisonController(ComparisonService comparisonService) {
        this.comparisonService = comparisonService;
    }

    /**
     * Get comparison data for a project
     * Returns: { requirement, diff, highlightedLines, feedback }
     */
    @GetMapping("/{projectId}")
    public ResponseEntity<ComparisonResponse> getComparisonData(
            @PathVariable Long projectId) {
        return ResponseEntity.ok(comparisonService.getComparisonData(projectId));
    }

    /**
     * Get AI feedback for a project
     * Returns: Array of feedback items with line numbers and severity
     */
    @GetMapping("/{projectId}/ai-feedback")
    public ResponseEntity<List<AIFeedbackItem>> getAIFeedback(
            @PathVariable Long projectId) {
        return ResponseEntity.ok(comparisonService.getAIFeedback(projectId));
    }

    /**
     * Get Jira requirement details
     */
    @GetMapping("/requirements/{requirementId}")
    public ResponseEntity<JiraRequirement> getJiraRequirement(
            @PathVariable String requirementId) {
        return ResponseEntity.ok(comparisonService.getJiraRequirement(requirementId));
    }

    /**
     * Get GitHub diff for a project
     * Query params: branch, baseBranch, filePath
     */
    @GetMapping("/{projectId}/diff")
    public ResponseEntity<String> getGitHubDiff(
            @PathVariable Long projectId,
            @RequestParam(required = false) String branch,
            @RequestParam(required = false) String baseBranch,
            @RequestParam(required = false) String filePath) {
        return ResponseEntity.ok(comparisonService.getGitHubDiff(projectId, branch, baseBranch, filePath));
    }

    /**
     * Update AI feedback dismissal or resolution status
     */
    @PatchMapping("/{projectId}/feedback/{feedbackId}")
    public ResponseEntity<Void> updateFeedbackStatus(
            @PathVariable Long projectId,
            @PathVariable String feedbackId,
            @RequestBody FeedbackStatusUpdate statusUpdate) {
        comparisonService.updateFeedbackStatus(projectId, feedbackId, statusUpdate.getStatus());
        return ResponseEntity.ok().build();
    }

    /**
     * Get comparison export (PDF or CSV)
     */
    @GetMapping("/{projectId}/export")
    public ResponseEntity<byte[]> exportComparison(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "pdf") String format) {
        // Placeholder - in production this would generate actual PDF/CSV
        String content = "Comparison export for project " + projectId;
        return ResponseEntity.ok()
                .header("Content-Type", format.equals("pdf") ? "application/pdf" : "text/csv")
                .header("Content-Disposition", "attachment; filename=comparison." + format)
                .body(content.getBytes());
    }
}
