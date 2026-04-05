package com.seniorapp.controller;

import com.seniorapp.dto.ValidStudentUploadRequest;
import com.seniorapp.entity.User;
import com.seniorapp.service.ValidStudentService;
import jakarta.validation.Valid;
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

    private final ValidStudentService validStudentService;

    public CoordinatorController(ValidStudentService validStudentService) {
        this.validStudentService = validStudentService;
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
            @AuthenticationPrincipal User principal) {

        String uploadedBy = principal != null ? principal.getEmail() : "system";
        int added = validStudentService.uploadStudentIds(request.getStudentIds(), uploadedBy);

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