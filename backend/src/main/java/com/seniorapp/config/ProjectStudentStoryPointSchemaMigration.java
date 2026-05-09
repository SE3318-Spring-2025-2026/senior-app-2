package com.seniorapp.config;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class ProjectStudentStoryPointSchemaMigration {
    public ProjectStudentStoryPointSchemaMigration(JdbcTemplate jdbcTemplate) {
        try {
            addColumnIfMissing(jdbcTemplate, "sprint_no", "INT NULL");
            addColumnIfMissing(jdbcTemplate, "accepted", "TINYINT(1) NOT NULL DEFAULT 0");
            addColumnIfMissing(jdbcTemplate, "accepted_by_user_id", "BIGINT NULL");
            addColumnIfMissing(jdbcTemplate, "accepted_at", "DATETIME NULL");

            jdbcTemplate.queryForList(
                    "SELECT index_name FROM information_schema.statistics " +
                            "WHERE table_schema = DATABASE() AND table_name = 'project_student_story_points' " +
                            "AND non_unique = 0 AND index_name <> 'PRIMARY'")
                    .forEach(row -> {
                        Object idx = row.get("index_name");
                        if (idx != null) {
                            try {
                                jdbcTemplate.execute(
                                        "ALTER TABLE project_student_story_points DROP INDEX " + idx);
                            } catch (Exception ignored) {
                                // best effort
                            }
                        }
                    });

            Integer idxCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.statistics " +
                            "WHERE table_schema = DATABASE() AND table_name = 'project_student_story_points' " +
                            "AND index_name = 'uk_pssp_project_student_sprint'",
                    Integer.class);
            if (idxCount != null && idxCount == 0) {
                jdbcTemplate.execute(
                        "ALTER TABLE project_student_story_points " +
                                "ADD UNIQUE KEY uk_pssp_project_student_sprint (project_id, student_user_id, sprint_no)");
            }
        } catch (Exception ignored) {
            // Best effort migration; do not block startup.
        }
    }

    private void addColumnIfMissing(JdbcTemplate jdbcTemplate, String columnName, String ddlType) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() " +
                        "AND table_name = 'project_student_story_points' AND column_name = ?",
                Integer.class,
                columnName);
        if (count != null && count == 0) {
            jdbcTemplate.execute("ALTER TABLE project_student_story_points ADD COLUMN " + columnName + " " + ddlType);
        }
    }
}
