package com.seniorapp.service;

import com.seniorapp.entity.AuditLog;
import com.seniorapp.repository.AuditLogRepository;
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
        AuditLog log = new AuditLog(userId, userRole, module, action, status, severity, message);
        return auditLogRepository.save(log);
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
        AuditLog log = new AuditLog(userId, userRole, module, action, status, severity, message);
        if (request != null) {
            log.setIpAddress(extractIp(request));
            log.setUserAgent(request.getHeader("User-Agent"));
            log.setHttpMethod(request.getMethod());
            log.setEndpoint(request.getRequestURI());
        }
        return auditLogRepository.save(log);
    }

    /**
     * Basit log — sadece modül, action ve mesaj (anonim/sistem logu).
     */
    public AuditLog saveLog(String module, String action, String message) {
        AuditLog log = new AuditLog();
        log.setModule(module);
        log.setAction(action);
        log.setMessage(message);
        log.setStatus("success");
        log.setSeverity("info");
        return auditLogRepository.save(log);
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
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setUserRole(userRole);
        log.setModule("security");
        log.setAction(action);
        log.setStatus(status);
        log.setSeverity("blocked".equals(status) ? "high" : "warning");
        log.setMessage(message);
        log.setSecurityEvent(true);
        log.setCriticalEvent(true);
        if (request != null) {
            log.setIpAddress(extractIp(request));
            log.setUserAgent(request.getHeader("User-Agent"));
            log.setHttpMethod(request.getMethod());
            log.setEndpoint(request.getRequestURI());
        }
        return auditLogRepository.save(log);
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
        AuditLog log = new AuditLog(userId, userRole, "authentication", action, status, severity, message);
        if (request != null) {
            log.setIpAddress(extractIp(request));
            log.setUserAgent(request.getHeader("User-Agent"));
            log.setHttpMethod(request.getMethod());
            log.setEndpoint(request.getRequestURI());
        }
        return auditLogRepository.save(log);
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
        AuditLog log = new AuditLog(userId, userRole, module, action, "failed", "high", message);
        log.setCriticalEvent(true);
        if (request != null) {
            log.setIpAddress(extractIp(request));
            log.setUserAgent(request.getHeader("User-Agent"));
            log.setHttpMethod(request.getMethod());
            log.setEndpoint(request.getRequestURI());
        }
        return auditLogRepository.save(log);
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
