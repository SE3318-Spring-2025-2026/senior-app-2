-- =========================================================
-- SENIOR PROJECT MANAGEMENT SYSTEM
-- AUDIT + SYSTEM LOGGING INFRASTRUCTURE
-- MySQL 8.0+
-- =========================================================

-- ---------------------------------------------------------
-- 1) DATABASE SELECTION
-- ---------------------------------------------------------
USE senior_project_management;

-- ---------------------------------------------------------
-- 2) SAFETY SETTINGS
-- ---------------------------------------------------------
SET NAMES utf8mb4;
SET time_zone = '+00:00';
SET sql_safe_updates = 0;

-- ---------------------------------------------------------
-- 3) OPTIONAL: DROP OBJECTS FOR CLEAN REINSTALL
-- ---------------------------------------------------------
DROP VIEW IF EXISTS vw_audit_log_recent;
DROP VIEW IF EXISTS vw_security_events;
DROP VIEW IF EXISTS vw_failed_integrations;
DROP VIEW IF EXISTS vw_grading_activity;
DROP VIEW IF EXISTS vw_submission_activity;

DROP PROCEDURE IF EXISTS sp_write_audit_log;
DROP PROCEDURE IF EXISTS sp_write_system_log;
DROP PROCEDURE IF EXISTS sp_log_auth_event;
DROP PROCEDURE IF EXISTS sp_log_security_event;
DROP PROCEDURE IF EXISTS sp_log_submission_event;
DROP PROCEDURE IF EXISTS sp_log_grading_event;
DROP PROCEDURE IF EXISTS sp_log_integration_event;
DROP PROCEDURE IF EXISTS sp_archive_old_logs;
DROP PROCEDURE IF EXISTS sp_delete_old_logs;

DROP TABLE IF EXISTS audit_log_archive;
DROP TABLE IF EXISTS system_logs;
DROP TABLE IF EXISTS audit_logs;

-- ---------------------------------------------------------
-- 4) MAIN AUDIT LOG TABLE
-- ---------------------------------------------------------
CREATE TABLE audit_logs (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    event_date DATE GENERATED ALWAYS AS (DATE(created_at)) STORED,

    user_id BIGINT UNSIGNED NULL,
    user_role VARCHAR(50) NULL,

    module VARCHAR(100) NOT NULL,
    action VARCHAR(120) NOT NULL,

    entity_type VARCHAR(100) NULL,
    entity_id VARCHAR(100) NULL,

    status VARCHAR(30) NOT NULL DEFAULT 'success',
    severity VARCHAR(20) NOT NULL DEFAULT 'info',

    message VARCHAR(1000) NOT NULL,

    request_id VARCHAR(120) NULL,
    session_id VARCHAR(120) NULL,

    ip_address VARCHAR(64) NULL,
    user_agent VARCHAR(500) NULL,

    http_method VARCHAR(10) NULL,
    endpoint VARCHAR(255) NULL,

    source_system VARCHAR(50) NOT NULL DEFAULT 'web_app',

    metadata_json JSON NULL,

    is_security_event TINYINT(1) NOT NULL DEFAULT 0,
    is_critical_event TINYINT(1) NOT NULL DEFAULT 0,

    PRIMARY KEY (id),

    CONSTRAINT chk_audit_status
        CHECK (status IN ('success', 'failed', 'blocked', 'warning', 'pending')),

    CONSTRAINT chk_audit_severity
        CHECK (severity IN ('debug', 'info', 'warning', 'high', 'critical')),

    CONSTRAINT chk_audit_security_bool
        CHECK (is_security_event IN (0,1)),

    CONSTRAINT chk_audit_critical_bool
        CHECK (is_critical_event IN (0,1))

    -- Eğer users tablon varsa aç:
    -- ,CONSTRAINT fk_audit_logs_user
    --    FOREIGN KEY (user_id) REFERENCES users(id)
    --    ON DELETE SET NULL
    --    ON UPDATE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------
-- 5) TECHNICAL / SYSTEM LOG TABLE
-- ---------------------------------------------------------
CREATE TABLE system_logs (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    event_date DATE GENERATED ALWAYS AS (DATE(created_at)) STORED,

    service_name VARCHAR(100) NOT NULL,
    module VARCHAR(100) NOT NULL,
    action VARCHAR(120) NOT NULL,

    status VARCHAR(30) NOT NULL DEFAULT 'success',
    severity VARCHAR(20) NOT NULL DEFAULT 'info',

    message VARCHAR(1000) NOT NULL,

    related_user_id BIGINT UNSIGNED NULL,
    related_entity_type VARCHAR(100) NULL,
    related_entity_id VARCHAR(100) NULL,

    source_system VARCHAR(50) NOT NULL DEFAULT 'backend',
    request_id VARCHAR(120) NULL,
    trace_id VARCHAR(120) NULL,

    error_code VARCHAR(100) NULL,
    error_stack TEXT NULL,

    external_provider VARCHAR(100) NULL,
    external_status_code VARCHAR(30) NULL,

    duration_ms INT UNSIGNED NULL,

    metadata_json JSON NULL,

    is_integration_event TINYINT(1) NOT NULL DEFAULT 0,
    is_retryable TINYINT(1) NOT NULL DEFAULT 0,

    PRIMARY KEY (id),

    CONSTRAINT chk_system_status
        CHECK (status IN ('success', 'failed', 'warning', 'timeout', 'pending')),

    CONSTRAINT chk_system_severity
        CHECK (severity IN ('debug', 'info', 'warning', 'high', 'critical')),

    CONSTRAINT chk_system_integration_bool
        CHECK (is_integration_event IN (0,1)),

    CONSTRAINT chk_system_retryable_bool
        CHECK (is_retryable IN (0,1))

    -- Eğer users tablon varsa aç:
    -- ,CONSTRAINT fk_system_logs_related_user
    --    FOREIGN KEY (related_user_id) REFERENCES users(id)
    --    ON DELETE SET NULL
    --    ON UPDATE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------
-- 6) OPTIONAL ARCHIVE TABLE
-- ---------------------------------------------------------
CREATE TABLE audit_log_archive (
    id BIGINT UNSIGNED NOT NULL,
    archived_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    created_at DATETIME(6) NOT NULL,
    event_date DATE NOT NULL,

    user_id BIGINT UNSIGNED NULL,
    user_role VARCHAR(50) NULL,

    module VARCHAR(100) NOT NULL,
    action VARCHAR(120) NOT NULL,

    entity_type VARCHAR(100) NULL,
    entity_id VARCHAR(100) NULL,

    status VARCHAR(30) NOT NULL,
    severity VARCHAR(20) NOT NULL,

    message VARCHAR(1000) NOT NULL,

    request_id VARCHAR(120) NULL,
    session_id VARCHAR(120) NULL,

    ip_address VARCHAR(64) NULL,
    user_agent VARCHAR(500) NULL,

    http_method VARCHAR(10) NULL,
    endpoint VARCHAR(255) NULL,

    source_system VARCHAR(50) NOT NULL,

    metadata_json JSON NULL,

    is_security_event TINYINT(1) NOT NULL,
    is_critical_event TINYINT(1) NOT NULL,

    PRIMARY KEY (id, archived_at)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------
-- 7) INDEXES FOR FAST AUDIT QUERIES
-- ---------------------------------------------------------
CREATE INDEX idx_audit_created_at
    ON audit_logs(created_at);

CREATE INDEX idx_audit_event_date
    ON audit_logs(event_date);

CREATE INDEX idx_audit_user_id
    ON audit_logs(user_id);

CREATE INDEX idx_audit_module_action
    ON audit_logs(module, action);

CREATE INDEX idx_audit_entity
    ON audit_logs(entity_type, entity_id);

CREATE INDEX idx_audit_status_severity
    ON audit_logs(status, severity);

CREATE INDEX idx_audit_security
    ON audit_logs(is_security_event, created_at);

CREATE INDEX idx_audit_critical
    ON audit_logs(is_critical_event, created_at);

CREATE INDEX idx_audit_request_id
    ON audit_logs(request_id);

CREATE INDEX idx_audit_endpoint
    ON audit_logs(endpoint);

CREATE INDEX idx_system_created_at
    ON system_logs(created_at);

CREATE INDEX idx_system_service_module
    ON system_logs(service_name, module);

CREATE INDEX idx_system_action_status
    ON system_logs(action, status);

CREATE INDEX idx_system_integration
    ON system_logs(is_integration_event, created_at);

CREATE INDEX idx_system_provider
    ON system_logs(external_provider, external_status_code);

CREATE INDEX idx_system_related_entity
    ON system_logs(related_entity_type, related_entity_id);

CREATE INDEX idx_system_trace_id
    ON system_logs(trace_id);

-- ---------------------------------------------------------
-- 8) VIEWS FOR COMMON AUDIT PANELS
-- ---------------------------------------------------------
CREATE VIEW vw_audit_log_recent AS
SELECT
    id,
    created_at,
    user_id,
    user_role,
    module,
    action,
    entity_type,
    entity_id,
    status,
    severity,
    message,
    endpoint,
    ip_address,
    is_security_event,
    is_critical_event
FROM audit_logs
WHERE created_at >= NOW() - INTERVAL 30 DAY
ORDER BY created_at DESC;

CREATE VIEW vw_security_events AS
SELECT
    id,
    created_at,
    user_id,
    user_role,
    module,
    action,
    status,
    severity,
    message,
    endpoint,
    ip_address,
    metadata_json
FROM audit_logs
WHERE is_security_event = 1
ORDER BY created_at DESC;

CREATE VIEW vw_failed_integrations AS
SELECT
    id,
    created_at,
    service_name,
    module,
    action,
    status,
    severity,
    message,
    external_provider,
    external_status_code,
    duration_ms,
    is_retryable
FROM system_logs
WHERE is_integration_event = 1
  AND status IN ('failed', 'timeout', 'warning')
ORDER BY created_at DESC;

CREATE VIEW vw_grading_activity AS
SELECT
    id,
    created_at,
    user_id,
    user_role,
    action,
    entity_type,
    entity_id,
    status,
    severity,
    message
FROM audit_logs
WHERE module = 'grading'
ORDER BY created_at DESC;

CREATE VIEW vw_submission_activity AS
SELECT
    id,
    created_at,
    user_id,
    user_role,
    action,
    entity_type,
    entity_id,
    status,
    severity,
    message
FROM audit_logs
WHERE module = 'submission'
ORDER BY created_at DESC;

-- ---------------------------------------------------------
-- 9) CORE PROCEDURE: WRITE AUDIT LOG
-- ---------------------------------------------------------
DELIMITER $$

CREATE PROCEDURE sp_write_audit_log (
    IN p_user_id BIGINT UNSIGNED,
    IN p_user_role VARCHAR(50),
    IN p_module VARCHAR(100),
    IN p_action VARCHAR(120),
    IN p_entity_type VARCHAR(100),
    IN p_entity_id VARCHAR(100),
    IN p_status VARCHAR(30),
    IN p_severity VARCHAR(20),
    IN p_message VARCHAR(1000),
    IN p_request_id VARCHAR(120),
    IN p_session_id VARCHAR(120),
    IN p_ip_address VARCHAR(64),
    IN p_user_agent VARCHAR(500),
    IN p_http_method VARCHAR(10),
    IN p_endpoint VARCHAR(255),
    IN p_source_system VARCHAR(50),
    IN p_metadata_json JSON,
    IN p_is_security_event TINYINT,
    IN p_is_critical_event TINYINT
)
BEGIN
    INSERT INTO audit_logs (
        user_id,
        user_role,
        module,
        action,
        entity_type,
        entity_id,
        status,
        severity,
        message,
        request_id,
        session_id,
        ip_address,
        user_agent,
        http_method,
        endpoint,
        source_system,
        metadata_json,
        is_security_event,
        is_critical_event
    )
    VALUES (
        p_user_id,
        p_user_role,
        p_module,
        p_action,
        p_entity_type,
        p_entity_id,
        p_status,
        p_severity,
        p_message,
        p_request_id,
        p_session_id,
        p_ip_address,
        p_user_agent,
        p_http_method,
        p_endpoint,
        COALESCE(p_source_system, 'web_app'),
        p_metadata_json,
        COALESCE(p_is_security_event, 0),
        COALESCE(p_is_critical_event, 0)
    );
END$$

DELIMITER ;

-- ---------------------------------------------------------
-- 10) CORE PROCEDURE: WRITE SYSTEM LOG
-- ---------------------------------------------------------
DELIMITER $$

CREATE PROCEDURE sp_write_system_log (
    IN p_service_name VARCHAR(100),
    IN p_module VARCHAR(100),
    IN p_action VARCHAR(120),
    IN p_status VARCHAR(30),
    IN p_severity VARCHAR(20),
    IN p_message VARCHAR(1000),
    IN p_related_user_id BIGINT UNSIGNED,
    IN p_related_entity_type VARCHAR(100),
    IN p_related_entity_id VARCHAR(100),
    IN p_source_system VARCHAR(50),
    IN p_request_id VARCHAR(120),
    IN p_trace_id VARCHAR(120),
    IN p_error_code VARCHAR(100),
    IN p_error_stack TEXT,
    IN p_external_provider VARCHAR(100),
    IN p_external_status_code VARCHAR(30),
    IN p_duration_ms INT UNSIGNED,
    IN p_metadata_json JSON,
    IN p_is_integration_event TINYINT,
    IN p_is_retryable TINYINT
)
BEGIN
    INSERT INTO system_logs (
        service_name,
        module,
        action,
        status,
        severity,
        message,
        related_user_id,
        related_entity_type,
        related_entity_id,
        source_system,
        request_id,
        trace_id,
        error_code,
        error_stack,
        external_provider,
        external_status_code,
        duration_ms,
        metadata_json,
        is_integration_event,
        is_retryable
    )
    VALUES (
        p_service_name,
        p_module,
        p_action,
        p_status,
        p_severity,
        p_message,
        p_related_user_id,
        p_related_entity_type,
        p_related_entity_id,
        COALESCE(p_source_system, 'backend'),
        p_request_id,
        p_trace_id,
        p_error_code,
        p_error_stack,
        p_external_provider,
        p_external_status_code,
        p_duration_ms,
        p_metadata_json,
        COALESCE(p_is_integration_event, 0),
        COALESCE(p_is_retryable, 0)
    );
END$$

DELIMITER ;

-- ---------------------------------------------------------
-- 11) HELPER PROCEDURE: AUTH EVENT
-- ---------------------------------------------------------
DELIMITER $$

CREATE PROCEDURE sp_log_auth_event (
    IN p_user_id BIGINT UNSIGNED,
    IN p_user_role VARCHAR(50),
    IN p_action VARCHAR(120),
    IN p_status VARCHAR(30),
    IN p_message VARCHAR(1000),
    IN p_ip_address VARCHAR(64),
    IN p_user_agent VARCHAR(500),
    IN p_request_id VARCHAR(120),
    IN p_metadata_json JSON
)
BEGIN
    CALL sp_write_audit_log(
        p_user_id,
        p_user_role,
        'authentication',
        p_action,
        'user',
        CAST(p_user_id AS CHAR),
        p_status,
        CASE
            WHEN p_status IN ('failed', 'blocked') THEN 'warning'
            ELSE 'info'
        END,
        p_message,
        p_request_id,
        NULL,
        p_ip_address,
        p_user_agent,
        'POST',
        '/auth',
        'web_app',
        p_metadata_json,
        0,
        1
    );
END$$

DELIMITER ;

-- ---------------------------------------------------------
-- 12) HELPER PROCEDURE: SECURITY EVENT
-- ---------------------------------------------------------
DELIMITER $$

CREATE PROCEDURE sp_log_security_event (
    IN p_user_id BIGINT UNSIGNED,
    IN p_user_role VARCHAR(50),
    IN p_action VARCHAR(120),
    IN p_entity_type VARCHAR(100),
    IN p_entity_id VARCHAR(100),
    IN p_status VARCHAR(30),
    IN p_message VARCHAR(1000),
    IN p_ip_address VARCHAR(64),
    IN p_user_agent VARCHAR(500),
    IN p_endpoint VARCHAR(255),
    IN p_request_id VARCHAR(120),
    IN p_metadata_json JSON
)
BEGIN
    CALL sp_write_audit_log(
        p_user_id,
        p_user_role,
        'security',
        p_action,
        p_entity_type,
        p_entity_id,
        p_status,
        CASE
            WHEN p_status = 'blocked' THEN 'high'
            WHEN p_status = 'failed' THEN 'warning'
            ELSE 'info'
        END,
        p_message,
        p_request_id,
        NULL,
        p_ip_address,
        p_user_agent,
        'POST',
        p_endpoint,
        'web_app',
        p_metadata_json,
        1,
        1
    );
END$$

DELIMITER ;

-- ---------------------------------------------------------
-- 13) HELPER PROCEDURE: SUBMISSION EVENT
-- ---------------------------------------------------------
DELIMITER $$

CREATE PROCEDURE sp_log_submission_event (
    IN p_user_id BIGINT UNSIGNED,
    IN p_user_role VARCHAR(50),
    IN p_action VARCHAR(120),
    IN p_entity_id VARCHAR(100),
    IN p_status VARCHAR(30),
    IN p_message VARCHAR(1000),
    IN p_request_id VARCHAR(120),
    IN p_metadata_json JSON
)
BEGIN
    CALL sp_write_audit_log(
        p_user_id,
        p_user_role,
        'submission',
        p_action,
        'deliverable',
        p_entity_id,
        p_status,
        CASE
            WHEN p_status = 'failed' THEN 'warning'
            WHEN p_status = 'blocked' THEN 'high'
            ELSE 'info'
        END,
        p_message,
        p_request_id,
        NULL,
        NULL,
        NULL,
        'POST',
        '/submission',
        'web_app',
        p_metadata_json,
        0,
        1
    );
END$$

DELIMITER ;

-- ---------------------------------------------------------
-- 14) HELPER PROCEDURE: GRADING EVENT
-- ---------------------------------------------------------
DELIMITER $$

CREATE PROCEDURE sp_log_grading_event (
    IN p_user_id BIGINT UNSIGNED,
    IN p_user_role VARCHAR(50),
    IN p_action VARCHAR(120),
    IN p_entity_id VARCHAR(100),
    IN p_status VARCHAR(30),
    IN p_message VARCHAR(1000),
    IN p_request_id VARCHAR(120),
    IN p_metadata_json JSON
)
BEGIN
    CALL sp_write_audit_log(
        p_user_id,
        p_user_role,
        'grading',
        p_action,
        'grade',
        p_entity_id,
        p_status,
        CASE
            WHEN p_status = 'failed' THEN 'warning'
            ELSE 'info'
        END,
        p_message,
        p_request_id,
        NULL,
        NULL,
        NULL,
        'POST',
        '/grading',
        'web_app',
        p_metadata_json,
        0,
        1
    );
END$$

DELIMITER ;

-- ---------------------------------------------------------
-- 15) HELPER PROCEDURE: INTEGRATION EVENT
-- ---------------------------------------------------------
DELIMITER $$

CREATE PROCEDURE sp_log_integration_event (
    IN p_service_name VARCHAR(100),
    IN p_module VARCHAR(100),
    IN p_action VARCHAR(120),
    IN p_status VARCHAR(30),
    IN p_message VARCHAR(1000),
    IN p_external_provider VARCHAR(100),
    IN p_external_status_code VARCHAR(30),
    IN p_duration_ms INT UNSIGNED,
    IN p_error_code VARCHAR(100),
    IN p_error_stack TEXT,
    IN p_request_id VARCHAR(120),
    IN p_trace_id VARCHAR(120),
    IN p_metadata_json JSON
)
BEGIN
    CALL sp_write_system_log(
        p_service_name,
        p_module,
        p_action,
        p_status,
        CASE
            WHEN p_status IN ('failed', 'timeout') THEN 'high'
            WHEN p_status = 'warning' THEN 'warning'
            ELSE 'info'
        END,
        p_message,
        NULL,
        NULL,
        NULL,
        'backend',
        p_request_id,
        p_trace_id,
        p_error_code,
        p_error_stack,
        p_external_provider,
        p_external_status_code,
        p_duration_ms,
        p_metadata_json,
        1,
        CASE
            WHEN p_status IN ('failed', 'timeout') THEN 1
            ELSE 0
        END
    );
END$$

DELIMITER ;

-- ---------------------------------------------------------
-- 16) ARCHIVE OLD AUDIT LOGS
-- ---------------------------------------------------------
DELIMITER $$

CREATE PROCEDURE sp_archive_old_logs (
    IN p_keep_days INT
)
BEGIN
    INSERT INTO audit_log_archive (
        id,
        created_at,
        event_date,
        user_id,
        user_role,
        module,
        action,
        entity_type,
        entity_id,
        status,
        severity,
        message,
        request_id,
        session_id,
        ip_address,
        user_agent,
        http_method,
        endpoint,
        source_system,
        metadata_json,
        is_security_event,
        is_critical_event
    )
    SELECT
        id,
        created_at,
        event_date,
        user_id,
        user_role,
        module,
        action,
        entity_type,
        entity_id,
        status,
        severity,
        message,
        request_id,
        session_id,
        ip_address,
        user_agent,
        http_method,
        endpoint,
        source_system,
        metadata_json,
        is_security_event,
        is_critical_event
    FROM audit_logs
    WHERE created_at < NOW() - INTERVAL p_keep_days DAY;

    DELETE FROM audit_logs
    WHERE created_at < NOW() - INTERVAL p_keep_days DAY;
END$$

DELIMITER ;

-- ---------------------------------------------------------
-- 17) HARD DELETE VERY OLD LOGS
-- ---------------------------------------------------------
DELIMITER $$

CREATE PROCEDURE sp_delete_old_logs (
    IN p_keep_audit_days INT,
    IN p_keep_system_days INT
)
BEGIN
    DELETE FROM audit_logs
    WHERE created_at < NOW() - INTERVAL p_keep_audit_days DAY;

    DELETE FROM system_logs
    WHERE created_at < NOW() - INTERVAL p_keep_system_days DAY;
END$$

DELIMITER ;

-- ---------------------------------------------------------
-- 18) SAMPLE INSERTS / EXAMPLES
-- ---------------------------------------------------------

-- Student GitHub login success
CALL sp_log_auth_event(
    101,
    'student',
    'github_login_success',
    'success',
    'Student logged in successfully using GitHub OAuth.',
    '192.168.1.10',
    'Mozilla/5.0',
    'req-auth-001',
    JSON_OBJECT(
        'provider', 'github',
        'github_username', 'studentdev01'
    )
);

-- Professor first login reset
CALL sp_log_auth_event(
    205,
    'professor',
    'first_login_password_reset',
    'success',
    'Professor completed mandatory first-login password reset.',
    '192.168.1.11',
    'Mozilla/5.0',
    'req-auth-002',
    JSON_OBJECT(
        'flow', 'initial_password_reset'
    )
);

-- Unauthorized grade update attempt
CALL sp_log_security_event(
    101,
    'student',
    'unauthorized_grade_update_attempt',
    'grade',
    '4501',
    'blocked',
    'Student attempted to modify grading endpoint without permission.',
    '192.168.1.10',
    'Mozilla/5.0',
    '/api/grades/update',
    'req-sec-001',
    JSON_OBJECT(
        'required_role', 'advisor',
        'actual_role', 'student'
    )
);

-- Proposal submitted
CALL sp_log_submission_event(
    101,
    'team_leader',
    'proposal_submitted',
    '9001',
    'success',
    'Group proposal submitted within active schedule window.',
    'req-sub-001',
    JSON_OBJECT(
        'group_id', 33,
        'deliverable_type', 'proposal'
    )
);

-- SoW rejected due to late submission
CALL sp_log_submission_event(
    101,
    'team_leader',
    'sow_submission_outside_schedule',
    '9002',
    'blocked',
    'Statement of Work submission rejected because schedule window was closed.',
    'req-sub-002',
    JSON_OBJECT(
        'group_id', 33,
        'deliverable_type', 'sow',
        'schedule_open', false
    )
);

-- Advisor grades Point A
CALL sp_log_grading_event(
    205,
    'advisor',
    'point_a_assigned',
    '4501',
    'success',
    'Advisor assigned Point A grade for sprint performance.',
    'req-grade-001',
    JSON_OBJECT(
        'group_id', 33,
        'point_a', 'A'
    )
);

-- Committee rubric grading
CALL sp_log_grading_event(
    301,
    'committee_member',
    'rubric_grade_assigned',
    '4502',
    'success',
    'Committee member assigned rubric-based deliverable grades.',
    'req-grade-002',
    JSON_OBJECT(
        'deliverable_id', 9001,
        'rubric_mode', 'soft'
    )
);

-- GitHub sync success
CALL sp_log_integration_event(
    'daily_sync_worker',
    'integration',
    'github_pr_sync',
    'success',
    'GitHub pull request synchronization completed successfully.',
    'github',
    '200',
    1840,
    NULL,
    NULL,
    'req-int-001',
    'trace-int-001',
    JSON_OBJECT(
        'repository', 'senior-project-team-a',
        'synced_pr_count', 14
    )
);

-- JIRA timeout
CALL sp_log_integration_event(
    'daily_sync_worker',
    'integration',
    'jira_issue_sync',
    'timeout',
    'JIRA issue synchronization timed out.',
    'jira',
    '504',
    180000,
    'JIRA_TIMEOUT',
    'Connection timeout after 180 seconds.',
    'req-int-002',
    'trace-int-002',
    JSON_OBJECT(
        'project_key', 'CENG499',
        'retry_scheduled', true
    )
);

-- ---------------------------------------------------------
-- 19) USEFUL ADMIN QUERIES
-- ---------------------------------------------------------

-- Last 50 critical audit events
SELECT
    id,
    created_at,
    user_id,
    user_role,
    module,
    action,
    status,
    severity,
    message
FROM audit_logs
WHERE is_critical_event = 1
ORDER BY created_at DESC
LIMIT 50;

-- All blocked security events in last 7 days
SELECT
    id,
    created_at,
    user_id,
    action,
    endpoint,
    ip_address,
    message
FROM audit_logs
WHERE is_security_event = 1
  AND status = 'blocked'
  AND created_at >= NOW() - INTERVAL 7 DAY
ORDER BY created_at DESC;

-- Submission failures
SELECT
    id,
    created_at,
    user_id,
    action,
    entity_id,
    message
FROM audit_logs
WHERE module = 'submission'
  AND status IN ('failed', 'blocked')
ORDER BY created_at DESC;

-- Grade modification history for one grade
SELECT
    id,
    created_at,
    user_id,
    user_role,
    action,
    status,
    message,
    metadata_json
FROM audit_logs
WHERE module = 'grading'
  AND entity_type = 'grade'
  AND entity_id = '4501'
ORDER BY created_at DESC;

-- Failed external integrations
SELECT
    id,
    created_at,
    service_name,
    action,
    external_provider,
    external_status_code,
    duration_ms,
    message
FROM system_logs
WHERE is_integration_event = 1
  AND status IN ('failed', 'timeout')
ORDER BY created_at DESC;

-- Most active modules in last 30 days
SELECT
    module,
    COUNT(*) AS total_events
FROM audit_logs
WHERE created_at >= NOW() - INTERVAL 30 DAY
GROUP BY module
ORDER BY total_events DESC;

-- User activity summary
SELECT
    user_id,
    user_role,
    COUNT(*) AS total_events
FROM audit_logs
WHERE user_id IS NOT NULL
GROUP BY user_id, user_role
ORDER BY total_events DESC;

-- ---------------------------------------------------------
-- 20) OPTIONAL EVENT SCHEDULER FOR CLEANUP
-- ---------------------------------------------------------
SET GLOBAL event_scheduler = ON;

DROP EVENT IF EXISTS ev_archive_audit_logs;
DROP EVENT IF EXISTS ev_cleanup_system_logs;

CREATE EVENT ev_archive_audit_logs
ON SCHEDULE EVERY 1 DAY
STARTS CURRENT_TIMESTAMP + INTERVAL 1 DAY
DO
    CALL sp_archive_old_logs(180);

CREATE EVENT ev_cleanup_system_logs
ON SCHEDULE EVERY 1 DAY
STARTS CURRENT_TIMESTAMP + INTERVAL 1 DAY
DO
    DELETE FROM system_logs
    WHERE created_at < NOW() - INTERVAL 90 DAY;

-- ---------------------------------------------------------
-- 21) FINAL NOTES
-- ---------------------------------------------------------
-- Recommended module values:
-- authentication
-- security
-- group_management
-- advisor_management
-- committee_management
-- submission
-- grading
-- integration
-- ai_audit
-- scheduling
-- administration

-- Recommended status values:
-- success
-- failed
-- blocked
-- warning
-- pending
-- timeout (system_logs only)

-- Recommended severity values:
-- debug
-- info
-- warning
-- high
-- critical
