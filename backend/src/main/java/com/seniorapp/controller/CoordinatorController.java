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

    @GetMapping("/valid-students")
    @PreAuthorize("hasAnyRole('COORDINATOR', 'ADMIN')")
    public ResponseEntity<?> listValidStudents() {
        return ResponseEntity.ok(validStudentService.listAllWhitelistEntries());
    }

    @DeleteMapping("/valid-students/{id}")
    @PreAuthorize("hasAnyRole('COORDINATOR', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteValidStudent(
            @PathVariable Long id,
            @AuthenticationPrincipal User principal,
            HttpServletRequest httpRequest) {
        
        validStudentService.deleteWhitelistEntry(id);
        String userEmail = (principal != null) ? principal.getEmail() : "system";
        
        try {
            logService.saveLog(
                principal != null ? principal.getId() : null,
                principal != null ? principal.getRole().name() : "COORDINATOR",
                "coordinator", "whitelist_delete", "success", "info",
                "Removed whitelist entry id=" + id, httpRequest
            );
        } catch (Exception e) {
            log.warn("Log kaydı oluşturulamadı", e);
        }
        return ResponseEntity.ok(Map.of("message", "Kayıt başarıyla silindi.", "id", id));
    }

    @PostMapping({"/valid-students", "/whitelist"})
    @PreAuthorize("hasAnyRole('COORDINATOR', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> uploadValidStudents(
            @Valid @RequestBody ValidStudentUploadRequest request,
            @AuthenticationPrincipal User principal,
            HttpServletRequest httpRequest) {

        String uploadedBy = (principal != null) ? principal.getEmail() : "system";
        int added = validStudentService.uploadStudentIds(request.getStudentIds(), uploadedBy);

        try {
            logService.saveLog(
                principal != null ? principal.getId() : null,
                principal != null ? principal.getRole().name() : "COORDINATOR",
                "coordinator", "whitelist_upload", "success", "info",
                "Added " + added + " new rows", httpRequest
            );
        } catch (Exception e) {
            log.warn("Log kaydı oluşturulamadı", e);
        }

        return ResponseEntity.ok(Map.of(
                "message", added + " yeni öğrenci eklendi.",
                "added", added,
                "total", request.getStudentIds().size()
        ));
    }

    @GetMapping("/valid-students/pending")
    @PreAuthorize("hasAnyRole('COORDINATOR', 'ADMIN')")
    public ResponseEntity<?> getPendingStudents() {
        return ResponseEntity.ok(validStudentService.getPendingEntries());
    }

    @GetMapping("/valid-students/registered")
    @PreAuthorize("hasAnyRole('COORDINATOR', 'ADMIN')")
    public ResponseEntity<?> getRegisteredStudents() {
        return ResponseEntity.ok(validStudentService.getRegisteredEntries());
    }
}