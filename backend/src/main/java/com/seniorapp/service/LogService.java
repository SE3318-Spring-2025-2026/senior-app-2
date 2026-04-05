package com.seniorapp.service;

import com.seniorapp.entity.AuditLog;
import com.seniorapp.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;

/**
 * LogService — sistem geneli log kayıt servisi.
 *
 * Kullanım örneği (herhangi bir controller'dan):
 *
 *   logService.saveLog(userId, "STUDENT", "authentication", "student_logged_in",
 *                      "success", "info", "Student logged in successfully");
 *
 *   // Daha kısa versiyon:
 *   logService.saveLog("authentication", "student_logged_in", "Student logged in");
 */
@Service
public class LogService {

    private static final Logger log = LoggerFactory.getLogger(LogService.class);

    private final AuditLogRepository auditLogRepository;

    public LogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    // -------------------------------------------------------
    // Ana save metodları
    // -------------------------------------------------------

    /**
     * Tam parametreli log kaydı.
     */
    public AuditLog saveLog(Long userId,
                            String userRole,
                            String module,
                            String action,
                            String status,
                            String severity,
                            String message) {
        AuditLog audit = new AuditLog(userId, userRole, module, action, status, severity, message);
        AuditLog saved = auditLogRepository.save(audit);
        log.debug("Audit persisted: module={} action={} status={}", module, action, status);
        return saved;
    }

    /**
     * HTTP isteği bilgileriyle tam log kaydı (IP, endpoint, method dahil).
     */
    public AuditLog saveLog(Long userId,
                            String userRole,
                            String module,
                            String action,
                            String status,
                            String severity,
                            String message,
                            HttpServletRequest request) {
        AuditLog audit = new AuditLog(userId, userRole, module, action, status, severity, message);
        if (request != null) {
            audit.setIpAddress(extractIp(request));
            audit.setUserAgent(request.getHeader("User-Agent"));
            audit.setHttpMethod(request.getMethod());
            audit.setEndpoint(request.getRequestURI());
        }
        AuditLog saved = auditLogRepository.save(audit);
        log.debug("Audit persisted: module={} action={} status={}", module, action, status);
        return saved;
    }

    /**
     * Basit log — sadece modül, action ve mesaj (anonim/sistem logu).
     */
    public AuditLog saveLog(String module, String action, String message) {
        AuditLog audit = new AuditLog();
        audit.setModule(module);
        audit.setAction(action);
        audit.setMessage(message);
        audit.setStatus("success");
        audit.setSeverity("info");
        AuditLog saved = auditLogRepository.save(audit);
        log.debug("Audit persisted: module={} action={}", module, action);
        return saved;
    }

    /**
     * Güvenlik olayı logu (is_security_event = true, is_critical_event = true).
     */
    public AuditLog saveSecurityLog(Long userId,
                                    String userRole,
                                    String action,
                                    String status,
                                    String message,
                                    HttpServletRequest request) {
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
        AuditLog saved = auditLogRepository.save(audit);
        log.info("Security audit: action={} status={}", action, status);
        return saved;
    }

    /**
     * Auth (login/logout/register) logu — kısa yol.
     */
    public AuditLog saveAuthLog(Long userId,
                                String userRole,
                                String action,
                                String status,
                                String message,
                                HttpServletRequest request) {
        String severity = "failed".equals(status) || "blocked".equals(status) ? "warning" : "info";
        AuditLog audit = new AuditLog(userId, userRole, "authentication", action, status, severity, message);
        if (request != null) {
            audit.setIpAddress(extractIp(request));
            audit.setUserAgent(request.getHeader("User-Agent"));
            audit.setHttpMethod(request.getMethod());
            audit.setEndpoint(request.getRequestURI());
        }
        AuditLog saved = auditLogRepository.save(audit);
        log.debug("Auth audit: action={} status={}", action, status);
        return saved;
    }

    /**
     * Hata / exception logu.
     */
    public AuditLog saveErrorLog(Long userId,
                                 String userRole,
                                 String module,
                                 String action,
                                 String message,
                                 HttpServletRequest request) {
        AuditLog audit = new AuditLog(userId, userRole, module, action, "failed", "high", message);
        audit.setCriticalEvent(true);
        if (request != null) {
            audit.setIpAddress(extractIp(request));
            audit.setUserAgent(request.getHeader("User-Agent"));
            audit.setHttpMethod(request.getMethod());
            audit.setEndpoint(request.getRequestURI());
        }
        AuditLog saved = auditLogRepository.save(audit);
        log.warn("Error audit: module={} action={}", module, action);
        return saved;
    }

    // -------------------------------------------------------
    // Yardımcı
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
