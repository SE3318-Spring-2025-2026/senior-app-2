package com.seniorapp.config;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class UserJiraProfileSchemaMigration {
    public UserJiraProfileSchemaMigration(JdbcTemplate jdbcTemplate) {
        addColumnIfMissing(jdbcTemplate, "jira_site_url_encrypted", "VARCHAR(2048) NULL");
        addColumnIfMissing(jdbcTemplate, "jira_api_token_encrypted", "VARCHAR(2048) NULL");
        addColumnIfMissing(jdbcTemplate, "jira_refresh_token_encrypted", "VARCHAR(2048) NULL");
        addColumnIfMissing(jdbcTemplate, "jira_account_id", "VARCHAR(255) NULL");
        addColumnIfMissing(jdbcTemplate, "jira_email", "VARCHAR(255) NULL");
        addColumnIfMissing(jdbcTemplate, "jira_display_name", "VARCHAR(255) NULL");
        widenTokenColumnsToText(jdbcTemplate);
    }

    private void addColumnIfMissing(JdbcTemplate jdbcTemplate, String columnName, String ddlType) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = ?",
                    Integer.class,
                    columnName);
            if (count != null && count == 0) {
                jdbcTemplate.execute("ALTER TABLE users ADD COLUMN " + columnName + " " + ddlType);
            }
        } catch (Exception ignored) {
            // Best effort schema migration.
        }
    }

    private void widenTokenColumnsToText(JdbcTemplate jdbcTemplate) {
        try {
            jdbcTemplate.execute("ALTER TABLE users MODIFY COLUMN jira_site_url_encrypted TEXT NULL");
        } catch (Exception ignored) {
        }
        try {
            jdbcTemplate.execute("ALTER TABLE users MODIFY COLUMN jira_api_token_encrypted TEXT NULL");
        } catch (Exception ignored) {
        }
        try {
            jdbcTemplate.execute("ALTER TABLE users MODIFY COLUMN jira_refresh_token_encrypted TEXT NULL");
        } catch (Exception ignored) {
        }
    }
}
