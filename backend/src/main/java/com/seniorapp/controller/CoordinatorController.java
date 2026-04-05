package com.seniorapp.controller;

import com.seniorapp.dto.ValidStudentUploadRequest;
import com.seniorapp.entity.User;
import com.seniorapp.service.LogService;
import com.seniorapp.service.ValidStudentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for Coordinator-level administrative operations.
 *
 * <p>All endpoints under this controller require the caller to have
 * the {@code COORDINATOR} or {@code ADMIN} role.</p>
 *
 * <p>Base path: {@code /api/coordinator}</p>
 */
@RestController
@RequestMapping("/api/coordinator")
public class CoordinatorController {

    private static final Logger log = LoggerFactory.getLogger(CoordinatorController.class);

    private final ValidStudentService validStudentService;
    private final LogService logService;

    public CoordinatorController(ValidStudentService validStudentService, LogService logService) {
        this.validStudentService = validStudentService;
        this.logService = logService;
    }

    // -------------------------------------------------------
    // GET /api/coordinator/valid-students  — list all (newest first)
    // -------------------------------------------------------
    @GetMapping("/valid-students")
    @PreAuthorize("hasAnyRole('COORDINATOR', 'ADMIN')")
    public ResponseEntity<?> listValidStudents() {
        return ResponseEntity.ok(validStudentService.listAllWhitelistEntries());
    }

    // -------------------------------------------------------
    // DELETE /api/coordinator/valid-students/{id}
    // -------------------------------------------------------
    @DeleteMapping("/valid-students/{id}")
    @PreAuthorize("hasAnyRole('COORDINATOR', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteValidStudent(
            @PathVariable Long id,
            @AuthenticationPrincipal User principal,
            HttpServletRequest httpRequest) {
        validStudentService.deleteWhitelistEntry(id);
        log.info("Whitelist entry deleted id={} by {}", id, principal != null ? principal.getEmail() : "system");
        try {
            logService.saveLog(
                    principal != null ? principal.getId() : null,
                    principal != null ? principal.getRole().name() : "COORDINATOR",
                    "coordinator",
                    "whitelist_delete",
                    "success",
                    "info",
                    "Removed whitelist entry id=" + id,
                    httpRequest
            );
        } catch (Exception e) {
            log.warn("Could not persist whitelist delete audit", e);
        }
        return ResponseEntity.ok(Map.of("message", "Whitelist entry removed.", "id", id));
    }

    // -------------------------------------------------------
    // POST /api/coordinator/valid-students
    // -------------------------------------------------------

    /**
     * Pre-approves a batch of student identifiers (e-mails or ID strings)
     * so that they can later authenticate via GitHub OAuth and have an
     * account created automatically.
     *
     * <p>Duplicate entries are silently skipped (idempotent operation).</p>
     *
     * <p>Request body example:</p>
     * <pre>{@code
     * { "studentIds": ["std001@uni.edu", "std002@uni.edu"] }
     * }</pre>
     *
     * @param request   validated request body containing the list of student IDs
     * @param principal the authenticated coordinator performing the upload
     * @return 200 OK with the count of newly added entries
     */
    /** Legacy path {@code /whitelist} kept for older clients; prefer {@code /valid-students}. */
    @PostMapping({"/valid-students", "/whitelist"})
    @PreAuthorize("hasAnyRole('COORDINATOR', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> uploadValidStudents(
            @Valid @RequestBody ValidStudentUploadRequest request,
            @AuthenticationPrincipal User principal,
            HttpServletRequest httpRequest) {

        String uploadedBy = principal != null ? principal.getEmail() : "system";
        int added = validStudentService.uploadStudentIds(request.getStudentIds(), uploadedBy);

        log.info("Whitelist upload: added={} batchSize={} by={}", added, request.getStudentIds().size(), uploadedBy);
        try {
            logService.saveLog(
                    principal != null ? principal.getId() : null,
                    principal != null ? principal.getRole().name() : "COORDINATOR",
                    "coordinator",
                    "whitelist_upload",
                    "success",
                    "info",
                    "Added " + added + " new whitelist row(s); batch size " + request.getStudentIds().size(),
                    httpRequest
            );
        } catch (Exception e) {
            log.warn("Could not persist whitelist upload audit entry", e);
        }

        return ResponseEntity.ok(Map.of(
                "message", added + " new student ID(s) added to the whitelist.",
                "added",   added,
                "total",   request.getStudentIds().size()
        ));
    }

    // -------------------------------------------------------
    // GET /api/coordinator/valid-students/pending
    // -------------------------------------------------------

    /**
     * Returns all whitelist entries that have not yet been linked to
     * an existing user account (i.e., the student has not logged in yet).
     *
     * @return 200 OK with a list of pending entries
     */
    @GetMapping("/valid-students/pending")
    @PreAuthorize("hasAnyRole('COORDINATOR', 'ADMIN')")
    public ResponseEntity<?> getPendingStudents() {
        return ResponseEntity.ok(validStudentService.getPendingEntries());
    }

    // -------------------------------------------------------
    // GET /api/coordinator/valid-students/registered
    // -------------------------------------------------------

    /**
     * Returns all whitelist entries that have already been linked to
     * an existing user account (i.e., the student has completed login at least once).
     *
     * @return 200 OK with a list of registered entries
     */
    @GetMapping("/valid-students/registered")
    @PreAuthorize("hasAnyRole('COORDINATOR', 'ADMIN')")
    public ResponseEntity<?> getRegisteredStudents() {
        return ResponseEntity.ok(validStudentService.getRegisteredEntries());
    }
}