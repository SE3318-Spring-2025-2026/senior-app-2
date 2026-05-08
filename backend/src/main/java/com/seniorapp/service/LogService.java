package com.seniorapp.service;

import com.seniorapp.entity.AuditLog;
import com.seniorapp.repository.AuditLogRepository;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * LogService — Sistem geneli log kayıt ve filtreleme servisi.
 */
@Service
public class LogService {

    private static final Logger log = LoggerFactory.getLogger(LogService.class);
    private final AuditLogRepository auditLogRepository;

    public LogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    // -------------------------------------------------------
    // OKUMA & FİLTRELEME METODU
    // -------------------------------------------------------

    /**
     * Frontend'den gelen dinamik filtrelere göre logları getirir.
     * JpaSpecificationExecutor kullanarak sadece dolu olan alanlara göre sorgu yapar.
     */
    public Page<AuditLog> getFilteredLogs(String module, String severity, String action, String status, String ipAddress, Pageable pageable) {
        return auditLogRepository.findAll((Specification<AuditLog>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Modül filtresi (LOWER case ile kısmi arama)
            if (module != null && !module.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("module")), "%" + module.toLowerCase() + "%"));
            }

            // Severity filtresi (Tam eşleşme)
            if (severity != null && !severity.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("severity"), severity));
            }

            // Aksiyon filtresi (LOWER case ile kısmi arama)
            if (action != null && !action.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("action")), "%" + action.toLowerCase() + "%"));
            }

            // Durum filtresi (Tam eşleşme)
            if (status != null && !status.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            // IP Adresi filtresi
            if (ipAddress != null && !ipAddress.trim().isEmpty()) {
                predicates.add(cb.like(root.get("ipAddress"), "%" + ipAddress + "%"));
            }

            // Her zaman en yeni kayıt en üstte gelsin
            query.orderBy(cb.desc(root.get("createdAt")));

            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);
    }

    // -------------------------------------------------------
    // KAYIT (SAVE) METODLARI
    // -------------------------------------------------------

    /** Tam parametreli log kaydı */
    public AuditLog saveLog(Long userId, String userRole, String module, String action, String status, String severity, String message) {
        AuditLog audit = new AuditLog(userId, userRole, module, action, status, severity, message);
        AuditLog saved = auditLogRepository.save(audit);
        log.debug("Audit persisted: module={} action={} status={}", module, action, status);
        return saved;
    }

    /** HTTP isteği bilgileriyle tam log kaydı */
    public AuditLog saveLog(Long userId, String userRole, String module, String action, String status, String severity, String message, HttpServletRequest request) {
        AuditLog audit = new AuditLog(userId, userRole, module, action, status, severity, message);
        if (request != null) {
            audit.setIpAddress(extractIp(request));
            audit.setUserAgent(request.getHeader("User-Agent"));
            audit.setHttpMethod(request.getMethod());
            audit.setEndpoint(request.getRequestURI());
        }
        AuditLog saved = auditLogRepository.save(audit);
        log.debug("Audit persisted with request info: module={} action={}", module, action);
        return saved;
    }

    /** Basit log — anonim/sistem logu */
    public AuditLog saveLog(String module, String action, String message) {
        AuditLog audit = new AuditLog();
        audit.setModule(module);
        audit.setAction(action);
        audit.setMessage(message);
        audit.setStatus("success");
        audit.setSeverity("info");
        return auditLogRepository.save(audit);
    }

    /** Güvenlik olayı logu */
    public AuditLog saveSecurityLog(Long userId, String userRole, String action, String status, String message, HttpServletRequest request) {
        AuditLog audit = new AuditLog();
        audit.setUserId(userId);
        audit.setUserRole(userRole);
        audit.setModule("security");
        audit.setAction(action);
        audit.setStatus(status);
        audit.setSeverity("blocked".equals(status) ? "high" : "warning");
        audit.setMessage(message);
        audit.setSecurityEvent(true);
        audit.setCriticalEvent(true);
        if (request != null) {
            audit.setIpAddress(extractIp(request));
            audit.setUserAgent(request.getHeader("User-Agent"));
            audit.setHttpMethod(request.getMethod());
            audit.setEndpoint(request.getRequestURI());
        }
        log.info("Security audit recorded: action={} status={}", action, status);
        return auditLogRepository.save(audit);
    }

    /** Auth (Login/Logout) logu */
    public AuditLog saveAuthLog(Long userId, String userRole, String action, String status, String message, HttpServletRequest request) {
        String severity = "failed".equals(status) || "blocked".equals(status) ? "warning" : "info";
        AuditLog audit = new AuditLog(userId, userRole, "authentication", action, status, severity, message);
        if (request != null) {
            audit.setIpAddress(extractIp(request));
            audit.setUserAgent(request.getHeader("User-Agent"));
            audit.setHttpMethod(request.getMethod());
            audit.setEndpoint(request.getRequestURI());
        }
        return auditLogRepository.save(audit);
    }

    /** Hata / Exception logu */
    public AuditLog saveErrorLog(Long userId, String userRole, String module, String action, String message, HttpServletRequest request) {
        AuditLog audit = new AuditLog(userId, userRole, module, action, "failed", "high", message);
        audit.setCriticalEvent(true);
        if (request != null) {
            audit.setIpAddress(extractIp(request));
            audit.setUserAgent(request.getHeader("User-Agent"));
            audit.setHttpMethod(request.getMethod());
            audit.setEndpoint(request.getRequestURI());
        }
        log.warn("Error audit recorded: module={} action={}", module, action);
        return auditLogRepository.save(audit);
    }

    // -------------------------------------------------------
    // YARDIMCI METODLAR
    // -------------------------------------------------------

    /** X-Forwarded-For başlığını destekleyen IP çıkarıcı */
    private String extractIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}