package com.seniorapp.config;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class ProjectJiraSchemaMigration {
    public ProjectJiraSchemaMigration(JdbcTemplate jdbcTemplate) {
        addColumnIfMissing(jdbcTemplate, "jira_project_key", "VARCHAR(255) NULL");
        addColumnIfMissing(jdbcTemplate, "jira_project_id", "VARCHAR(255) NULL");
        addColumnIfMissing(jdbcTemplate, "jira_board_id", "VARCHAR(255) NULL");
    }

    private void addColumnIfMissing(JdbcTemplate jdbcTemplate, String columnName, String ddlType) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'projects' AND column_name = ?",
                    Integer.class,
                    columnName);
            if (count != null && count == 0) {
                jdbcTemplate.execute("ALTER TABLE projects ADD COLUMN " + columnName + " " + ddlType);
            }
        } catch (Exception ignored) {
            // Best effort schema migration.
        }
    }
}
