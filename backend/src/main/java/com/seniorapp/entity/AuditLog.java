package com.seniorapp.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private Long userId;

    @Column(length = 50)
    private String userRole;

    @Column(nullable = false, length = 100)
    private String module;

    @Column(nullable = false, length = 120)
    private String action;

    @Column(length = 100)
    private String entityType;

    @Column(length = 100)
    private String entityId;

    @Column(nullable = false, length = 30)
    private String status = "success";

    @Column(nullable = false, length = 20)
    private String severity = "info";

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(length = 120)
    private String requestId;

    @Column(length = 120)
    private String sessionId;

    @Column(length = 64)
    private String ipAddress;

    @Column(length = 500)
    private String userAgent;

    @Column(length = 10)
    private String httpMethod;

    @Column(length = 255)
    private String endpoint;

    @Column(nullable = false, length = 50)
    private String sourceSystem = "web_app";

    @Column(columnDefinition = "JSON")
    private String metadataJson;

    @Column(nullable = false)
    private boolean isSecurityEvent = false;

    @Column(nullable = false)
    private boolean isCriticalEvent = false;

    @Column(name = "score")
    private Double score;

    // -------------------------------------------------------
    // Constructors
    // -------------------------------------------------------

    public AuditLog() {}

    /** Quick constructor for simple log entries */
    public AuditLog(Long userId, String userRole, String module, String action,
                    String status, String severity, String message) {
        this.userId       = userId;
        this.userRole     = userRole;
        this.module       = module;
        this.action       = action;
        this.status       = status;
        this.severity     = severity;
        this.message      = message;
    }

    // -------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------

    public Long getId()                         { return id; }
    public void setId(Long id)                  { this.id = id; }

    public LocalDateTime getCreatedAt()                         { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt)           { this.createdAt = createdAt; }

    public Long getUserId()                     { return userId; }
    public void setUserId(Long userId)          { this.userId = userId; }

    public String getUserRole()                 { return userRole; }
    public void setUserRole(String userRole)    { this.userRole = userRole; }

    public String getModule()                   { return module; }
    public void setModule(String module)        { this.module = module; }

    public String getAction()                   { return action; }
    public void setAction(String action)        { this.action = action; }

    public String getEntityType()               { return entityType; }
    public void setEntityType(String entityType){ this.entityType = entityType; }

    public String getEntityId()                 { return entityId; }
    public void setEntityId(String entityId)    { this.entityId = entityId; }

    public String getStatus()                   { return status; }
    public void setStatus(String status)        { this.status = status; }

    public String getSeverity()                 { return severity; }
    public void setSeverity(String severity)    { this.severity = severity; }

    public String getMessage()                  { return message; }
    public void setMessage(String message)      { this.message = message; }

    public String getRequestId()                { return requestId; }
    public void setRequestId(String requestId)  { this.requestId = requestId; }

    public String getSessionId()                { return sessionId; }
    public void setSessionId(String sessionId)  { this.sessionId = sessionId; }

    public String getIpAddress()                { return ipAddress; }
    public void setIpAddress(String ipAddress)  { this.ipAddress = ipAddress; }

    public String getUserAgent()                { return userAgent; }
    public void setUserAgent(String userAgent)  { this.userAgent = userAgent; }

    public String getHttpMethod()               { return httpMethod; }
    public void setHttpMethod(String httpMethod){ this.httpMethod = httpMethod; }

    public String getEndpoint()                 { return endpoint; }
    public void setEndpoint(String endpoint)    { this.endpoint = endpoint; }

    public String getSourceSystem()                     { return sourceSystem; }
    public void setSourceSystem(String sourceSystem)    { this.sourceSystem = sourceSystem; }

    public String getMetadataJson()                     { return metadataJson; }
    public void setMetadataJson(String metadataJson)    { this.metadataJson = metadataJson; }

    public boolean isSecurityEvent()                        { return isSecurityEvent; }
    public void setSecurityEvent(boolean securityEvent)     { this.isSecurityEvent = securityEvent; }

    public boolean isCriticalEvent()                        { return isCriticalEvent; }
    public void setCriticalEvent(boolean criticalEvent)     { this.isCriticalEvent = criticalEvent; }

    public Double getScore()                                { return score; }
    public void setScore(Double score)                      { this.score = score; }
}