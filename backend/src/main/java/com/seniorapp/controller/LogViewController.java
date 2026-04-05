package com.seniorapp.controller;

import com.seniorapp.dto.LogResponse;
import com.seniorapp.entity.AuditLog;
import com.seniorapp.entity.User;
import com.seniorapp.repository.AuditLogRepository;
import com.seniorapp.service.LogService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * LogViewController — Log inceleme sayfası için REST controller.
 *
 * Tüm endpoint'ler ADMIN veya ADVISOR rolü gerektirir.
 *
 * Base URL: /api/logs
 */
@RestController
@RequestMapping("/api/logs")
@PreAuthorize("hasAnyRole('ADMIN', 'ADVISOR')")
public class LogViewController {

    private static final Logger log = LoggerFactory.getLogger(LogViewController.class);

    private final AuditLogRepository auditLogRepository;
    private final LogService logService;

    public LogViewController(AuditLogRepository auditLogRepository, LogService logService) {
        this.auditLogRepository = auditLogRepository;
        this.logService = logService;
    }

    // -------------------------------------------------------
    // GET /api/logs  — Tüm loglar (sayfalı)
    // -------------------------------------------------------
    @GetMapping
    public ResponseEntity<Page<LogResponse>> getAllLogs(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<LogResponse> result = auditLogRepository
                .findAllByOrderByCreatedAtDesc(pageable)
                .map(LogResponse::from);
        return ResponseEntity.ok(result);
    }

    // -------------------------------------------------------
    // GET /api/logs/{id}  — Tek log detayı
    // -------------------------------------------------------
    @GetMapping("/{id}")
    public ResponseEntity<LogResponse> getLogById(@PathVariable Long id) {
        return auditLogRepository.findById(id)
                .map(log -> ResponseEntity.ok(LogResponse.from(log)))
                .orElse(ResponseEntity.notFound().build());
    }

    // -------------------------------------------------------
    // GET /api/logs/security  — Sadece güvenlik olayları
    // -------------------------------------------------------
    @GetMapping("/security")
    public ResponseEntity<Page<LogResponse>> getSecurityLogs(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<LogResponse> result = auditLogRepository
                .findByIsSecurityEventTrueOrderByCreatedAtDesc(pageable)
                .map(LogResponse::from);
        return ResponseEntity.ok(result);
    }

    // -------------------------------------------------------
    // GET /api/logs/critical  — Sadece kritik olaylar
    // -------------------------------------------------------
    @GetMapping("/critical")
    public ResponseEntity<Page<LogResponse>> getCriticalLogs(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<LogResponse> result = auditLogRepository
                .findByIsCriticalEventTrueOrderByCreatedAtDesc(pageable)
                .map(LogResponse::from);
        return ResponseEntity.ok(result);
    }

    // -------------------------------------------------------
    // GET /api/logs/filter  — Modül ve/veya severity filtresi
    //   Örnek: /api/logs/filter?module=authentication&severity=warning
    // -------------------------------------------------------
    @GetMapping("/filter")
    public ResponseEntity<Page<LogResponse>> filterLogs(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<AuditLog> rawPage;

        // Kullanıcıya göre filtreleme
        if (userId != null) {
            rawPage = auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }
        // Modül + severity kombinasyonu
        else if (module != null && severity != null) {
            rawPage = auditLogRepository.findByModuleAndSeverityOrderByCreatedAtDesc(module, severity, pageable);
        }
        // Modül + status kombinasyonu
        else if (module != null && status != null) {
            rawPage = auditLogRepository.findByModuleAndStatusOrderByCreatedAtDesc(module, status, pageable);
        }
        // Sadece modül
        else if (module != null) {
            rawPage = auditLogRepository.findByModuleOrderByCreatedAtDesc(module, pageable);
        }
        // Sadece severity
        else if (severity != null) {
            rawPage = auditLogRepository.findBySeverityOrderByCreatedAtDesc(severity, pageable);
        }
        // Filtre yok → tümünü getir
        else {
            rawPage = auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        return ResponseEntity.ok(rawPage.map(LogResponse::from));
    }

    // -------------------------------------------------------
    // GET /api/logs/range  — Tarih aralığı filtresi
    //   Örnek: /api/logs/range?from=2026-04-01T00:00:00&to=2026-04-02T00:00:00
    // -------------------------------------------------------
    @GetMapping("/range")
    public ResponseEntity<Page<LogResponse>> getLogsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<LogResponse> result = auditLogRepository
                .findByCreatedAtBetweenOrderByCreatedAtDesc(from, to, pageable)
                .map(LogResponse::from);
        return ResponseEntity.ok(result);
    }

    // -------------------------------------------------------
    // GET /api/logs/stats  — İstatistik özeti
    // -------------------------------------------------------
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getLogStats() {
        long totalLogs      = auditLogRepository.count();
        long securityEvents = auditLogRepository.countByIsSecurityEventTrue();
        long warningLogs    = auditLogRepository.countBySeverity("warning");
        long criticalLogs   = auditLogRepository.countBySeverity("critical");
        long authLogs       = auditLogRepository.countByModule("authentication");

        return ResponseEntity.ok(Map.of(
                "totalLogs",      totalLogs,
                "securityEvents", securityEvents,
                "warningLogs",    warningLogs,
                "criticalLogs",   criticalLogs,
                "authLogs",       authLogs
        ));
    }

    // -------------------------------------------------------
    // DELETE /api/logs/cleanup?days=90  — Eski logları sil (ADMIN only)
    // -------------------------------------------------------
    @DeleteMapping("/cleanup")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> cleanupOldLogs(
            @RequestParam(defaultValue = "90") int days,
            @AuthenticationPrincipal User adminUser,
            HttpServletRequest httpRequest) {

        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        int deleted = auditLogRepository.deleteOlderThan(cutoff);

        log.warn("Audit log cleanup: deleted {} row(s) older than {} days", deleted, days);
        try {
            logService.saveLog(
                    adminUser != null ? adminUser.getId() : null,
                    adminUser != null ? adminUser.getRole().name() : "ADMIN",
                    "log_management",
                    "audit_cleanup",
                    "success",
                    "info",
                    "Purged " + deleted + " audit row(s) older than " + days + " days",
                    httpRequest
            );
        } catch (Exception e) {
            log.warn("Could not persist cleanup audit row", e);
        }

        return ResponseEntity.ok(Map.of(
                "message", "Cleanup completed",
                "deletedCount", deleted,
                "cutoffDate", cutoff.toString()
        ));
    }

    // -------------------------------------------------------
    // Exception handler
    // -------------------------------------------------------
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleError(RuntimeException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
