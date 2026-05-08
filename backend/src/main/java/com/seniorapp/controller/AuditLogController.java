package com.seniorapp.controller;

import com.seniorapp.entity.AuditLog;
import com.seniorapp.service.LogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final LogService logService;

    public AuditLogController(LogService logService) {
        this.logService = logService;
    }

    @GetMapping
    public ResponseEntity<Page<AuditLog>> getLogs(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String ipAddress,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Tarihe göre azalan (en yeni en üstte) sıralama yapıyoruz
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        Page<AuditLog> result = logService.getFilteredLogs(module, severity, action, status, ipAddress, pageable);
        
        return ResponseEntity.ok(result);
    }
}