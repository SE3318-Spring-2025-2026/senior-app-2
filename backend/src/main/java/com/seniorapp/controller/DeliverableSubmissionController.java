package com.seniorapp.controller;

import com.seniorapp.entity.DeliverableSubmission;
import com.seniorapp.entity.SubmissionType;
import com.seniorapp.entity.User;
import com.seniorapp.service.DeliverableSubmissionService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * REST controller for managing deliverable submissions.
 * Supports file upload (multipart) and text editor submissions.
 *
 * Base path: /api/submissions
 */
@RestController
@RequestMapping("/api/submissions")
public class DeliverableSubmissionController {

    private final DeliverableSubmissionService submissionService;

    public DeliverableSubmissionController(DeliverableSubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    /**
     * POST /api/submissions/file
     * Dosya yükleme ile submission oluşturur.
     * multipart/form-data formatında: file, deliverableId, groupId
     */
    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("deliverableId") Long deliverableId,
            @RequestParam("groupId") Long groupId,
            @AuthenticationPrincipal User principal
    ) {
        // Validate file is not empty
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "File cannot be empty. Please select a valid file."
            ));
        }

        DeliverableSubmission submission = submissionService.submitFile(
                deliverableId, groupId, principal.getId(), file);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "File uploaded successfully.",
                "data", submissionService.toDto(submission)
        ));
    }

    /**
     * POST /api/submissions/text
     * Metin editörü ile submission oluşturur.
     * JSON body: { deliverableId, groupId, textContent }
     */
    @PostMapping("/text")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Map<String, Object>> submitText(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal User principal
    ) {
        Long deliverableId = toLong(body.get("deliverableId"));
        Long groupId = toLong(body.get("groupId"));
        String textContent = (String) body.get("textContent");

        if (deliverableId == null || groupId == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Both deliverableId and groupId are required."
            ));
        }

        if (textContent == null || textContent.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Text content cannot be empty."
            ));
        }

        DeliverableSubmission submission = submissionService.submitText(
                deliverableId, groupId, principal.getId(), textContent);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Text submitted successfully.",
                "data", submissionService.toDto(submission)
        ));
    }

    /**
     * GET /api/submissions/{deliverableId}/group/{groupId}
     * Belirli bir deliverable ve grup için olan submission'ı getirir.
     */
    @GetMapping("/{deliverableId}/group/{groupId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'COORDINATOR', 'PROFESSOR', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getSubmission(
            @PathVariable Long deliverableId,
            @PathVariable Long groupId
    ) {
        Optional<DeliverableSubmission> submission = submissionService.getSubmission(deliverableId, groupId);
        if (submission.isPresent()) {
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", submissionService.toDto(submission.get())
            ));
        } else {
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", Map.of()
            ));
        }
    }

    /**
     * GET /api/submissions/project/{projectId}/group/{groupId}
     * Bir projeye ait belirli grubun tüm submission'larını getirir.
     */
    @GetMapping("/project/{projectId}/group/{groupId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'COORDINATOR', 'PROFESSOR', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getProjectSubmissions(
            @PathVariable Long projectId,
            @PathVariable Long groupId
    ) {
        List<DeliverableSubmission> submissions = submissionService.getSubmissionsByProject(projectId, groupId);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", submissionService.toDtoList(submissions)
        ));
    }

    /**
     * GET /api/submissions/{submissionId}/download
     * Submission'a ait dosyayı indirir.
     * Content-Disposition header'ında orijinal dosya adı döndürülür.
     */
    @GetMapping("/{submissionId}/download")
    @PreAuthorize("hasAnyRole('STUDENT', 'COORDINATOR', 'PROFESSOR', 'ADMIN')")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long submissionId) {
        DeliverableSubmission submission = submissionService.getSubmissionById(submissionId)
                .orElseThrow(() -> new NoSuchElementException("Submission not found: " + submissionId));

        Resource resource = submissionService.downloadFile(submissionId);

        // Return with original file name in Content-Disposition
        String fileName = submission.getOriginalFileName();
        if (fileName == null || fileName.isBlank()) {
            fileName = "download";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + sanitizeFileName(fileName) + "\"")
                .body(resource);
    }

    /**
     * DELETE /api/submissions/{submissionId}
     * Submission'ı siler. Sadece grubun Team Leader'ı silebilir.
     * Dosya tipindeyse disk'ten de silinir.
     */
    @DeleteMapping("/{submissionId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Map<String, Object>> deleteSubmission(
            @PathVariable Long submissionId,
            @AuthenticationPrincipal User principal
    ) {
        submissionService.deleteSubmission(submissionId, principal.getId());
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Submission deleted successfully."
        ));
    }

    // ── Exception Handlers ──

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleValidation(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException e) {
        return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntime(RuntimeException e) {
        return ResponseEntity.status(500).body(Map.of(
                "error", "An unexpected error occurred: " + e.getMessage()
        ));
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Sanitizes file name for Content-Disposition header.
     * Removes potentially dangerous characters.
     */
    private String sanitizeFileName(String fileName) {
        if (fileName == null) return "download";
        // Remove path separators, control chars, and quotes
        return fileName.replaceAll("[\\\\/:*?\"<>|\\x00-\\x1F]", "_");
    }
}
