package com.seniorapp.controller;

import com.seniorapp.entity.DeliverableSubmission;
import com.seniorapp.entity.User;
import com.seniorapp.service.DeliverableSubmissionService;
import com.seniorapp.service.DeliverableSubmissionService.FileDownload;
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
     * multipart/form-data: file, deliverableId, groupId
     */
    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("deliverableId") Long deliverableId,
            @RequestParam("groupId") Long groupId,
            @AuthenticationPrincipal User principal
    ) {
        DeliverableSubmission submission = submissionService.submitFile(
                deliverableId, groupId, principal.getId(), file);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Dosya başarıyla yüklendi.",
                "data", submissionService.toDto(submission)
        ));
    }

    /**
     * POST /api/submissions/text
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
            return ResponseEntity.badRequest().body(Map.of("error", "deliverableId ve groupId zorunludur."));
        }

        DeliverableSubmission submission = submissionService.submitText(
                deliverableId, groupId, principal.getId(), textContent);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Metin başarıyla kaydedildi.",
                "data", submissionService.toDto(submission)
        ));
    }

    /**
     * GET /api/submissions/{deliverableId}/group/{groupId}
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
     * GET /api/submissions/files/{fileId}/download
     */
    @GetMapping("/files/{fileId}/download")
    @PreAuthorize("hasAnyRole('STUDENT', 'COORDINATOR', 'PROFESSOR', 'ADMIN')")
    public ResponseEntity<Resource> downloadFileByFileId(
            @PathVariable Long fileId,
            @AuthenticationPrincipal User principal
    ) {
        FileDownload fd = submissionService.downloadFileByFileId(fileId, principal);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDispositionAttachment(fd.filename()))
                .body(fd.resource());
    }

    /**
     * GET /api/submissions/{submissionId}/download — latest file or legacy single file.
     */
    @GetMapping("/{submissionId}/download")
    @PreAuthorize("hasAnyRole('STUDENT', 'COORDINATOR', 'PROFESSOR', 'ADMIN')")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable Long submissionId,
            @AuthenticationPrincipal User principal
    ) {
        FileDownload fd = submissionService.downloadFile(submissionId, principal);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDispositionAttachment(fd.filename()))
                .body(fd.resource());
    }

    /**
     * DELETE /api/submissions/files/{fileId}
     */
    @DeleteMapping("/files/{fileId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Map<String, Object>> deleteSubmissionFile(
            @PathVariable Long fileId,
            @AuthenticationPrincipal User principal
    ) {
        submissionService.deleteSubmissionFile(fileId, principal.getId());
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Dosya başarıyla silindi."
        ));
    }

    /**
     * DELETE /api/submissions/{submissionId}
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
                "message", "Submission başarıyla silindi."
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleValidation(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(NoSuchElementException e) {
        return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
    }

    private static String contentDispositionAttachment(String filename) {
        if (filename == null || filename.isBlank()) {
            filename = "download";
        }
        String safe = filename.replace("\"", "'");
        return "attachment; filename=\"" + safe + "\"";
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
}
