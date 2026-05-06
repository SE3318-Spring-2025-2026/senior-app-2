package com.seniorapp.config;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class ProjectSchemaCompatibilityMigration {

    public ProjectSchemaCompatibilityMigration(JdbcTemplate jdbcTemplate) {
        Set<String> activeModelColumns = Set.of(
                "id",
                "template_id",
                "title",
                "term",
                "status",
                "created_by_user_id",
                "group_id",
                "created_at",
                "updated_at"
        );

        // information_schema differs between DBs (e.g., H2 does not expose MySQL's `extra` column).
        // This migration is best-effort; if the metadata query is incompatible, skip it.
        List<String> legacyRequiredColumns;
        try {
            legacyRequiredColumns = jdbcTemplate.query(
                    "SELECT column_name " +
                            "FROM information_schema.columns " +
                            "WHERE table_schema = DATABASE() " +
                            "  AND table_name = 'projects' " +
                            "  AND is_nullable = 'NO' " +
                            "  AND column_default IS NULL",
                    (rs, rowNum) -> rs.getString("column_name")
            );
        } catch (Exception ex) {
            // Don't fail application startup/tests due to schema inspection differences.
            return;
        }

        for (String columnName : legacyRequiredColumns) {
            if (!activeModelColumns.contains(columnName)) {
                makeNullableIfRequired(jdbcTemplate, columnName);
            }
        }
    }

    private void makeNullableIfRequired(JdbcTemplate jdbcTemplate, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'projects' AND column_name = ?",
                Integer.class,
                columnName
        );
        if (count == null || count == 0) {
            return;
        }
        String isNullable = jdbcTemplate.queryForObject(
                "SELECT is_nullable FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'projects' AND column_name = ?",
                String.class,
                columnName
        );
        String columnType = jdbcTemplate.queryForObject(
                "SELECT column_type FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'projects' AND column_name = ?",
                String.class,
                columnName
        );
        if ("YES".equalsIgnoreCase(isNullable) || columnType == null || columnType.isBlank()) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE projects MODIFY COLUMN " + columnName + " " + columnType + " NULL");
    }
}
