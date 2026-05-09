package com.seniorapp.config;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class ProjectGroupAssignmentCommitteeSchemaMigration {
    public ProjectGroupAssignmentCommitteeSchemaMigration(JdbcTemplate jdbcTemplate) {
        try {
            Integer columnCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'project_group_assignments' AND column_name = 'committee_id'",
                    Integer.class);
            if (columnCount != null && columnCount == 0) {
                jdbcTemplate.execute("ALTER TABLE project_group_assignments ADD COLUMN committee_id BIGINT NULL");
            }

            Integer fkCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'project_group_assignments' AND constraint_name = 'fk_pga_committee'",
                    Integer.class);
            if (fkCount != null && fkCount == 0) {
                jdbcTemplate.execute(
                        "ALTER TABLE project_group_assignments ADD CONSTRAINT fk_pga_committee FOREIGN KEY (committee_id) REFERENCES project_committees(id)");
            }
        } catch (Exception ignored) {
            // Best effort migration; do not block startup.
        }
    }
}
