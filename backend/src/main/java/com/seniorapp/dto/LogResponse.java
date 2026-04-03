package com.seniorapp.dto;

import com.seniorapp.entity.AuditLog;
import java.time.LocalDateTime;

public class LogResponse {

    private Long id;
    private LocalDateTime createdAt;
    private Long userId;
    private String userRole;
    private String module;
    private String action;
    private String entityType;
    private String entityId;
    private String status;
    private String severity;
    private String message;
    private String ipAddress;
    private String endpoint;
    private String httpMethod;
    private String sourceSystem;
    private boolean isSecurityEvent;
    private boolean isCriticalEvent;

    public LogResponse() {}

    /** Map from entity */
    public static LogResponse from(AuditLog log) {
        LogResponse r = new LogResponse();
        r.id              = log.getId();
        r.createdAt       = log.getCreatedAt();
        r.userId          = log.getUserId();
        r.userRole        = log.getUserRole();
        r.module          = log.getModule();
        r.action          = log.getAction();
        r.entityType      = log.getEntityType();
        r.entityId        = log.getEntityId();
        r.status          = log.getStatus();
        r.severity        = log.getSeverity();
        r.message         = log.getMessage();
        r.ipAddress       = log.getIpAddress();
        r.endpoint        = log.getEndpoint();
        r.httpMethod      = log.getHttpMethod();
        r.sourceSystem    = log.getSourceSystem();
        r.isSecurityEvent = log.isSecurityEvent();
        r.isCriticalEvent = log.isCriticalEvent();
        return r;
    }

    // Getters
    public Long getId()                 { return id; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Long getUserId()             { return userId; }
    public String getUserRole()         { return userRole; }
    public String getModule()           { return module; }
    public String getAction()           { return action; }
    public String getEntityType()       { return entityType; }
    public String getEntityId()         { return entityId; }
    public String getStatus()           { return status; }
    public String getSeverity()         { return severity; }
    public String getMessage()          { return message; }
    public String getIpAddress()        { return ipAddress; }
    public String getEndpoint()         { return endpoint; }
    public String getHttpMethod()       { return httpMethod; }
    public String getSourceSystem()     { return sourceSystem; }
    public boolean isSecurityEvent()    { return isSecurityEvent; }
    public boolean isCriticalEvent()    { return isCriticalEvent; }
}
