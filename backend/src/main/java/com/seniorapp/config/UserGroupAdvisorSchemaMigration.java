package com.seniorapp.config;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class UserGroupAdvisorSchemaMigration {

    public UserGroupAdvisorSchemaMigration(JdbcTemplate jdbcTemplate) {
        try {
            Integer advisorCol = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'user_groups' AND column_name = 'advisor_id'",
                    Integer.class);
            if (advisorCol != null && advisorCol == 0) {
                jdbcTemplate.execute("ALTER TABLE user_groups ADD COLUMN advisor_id BIGINT NULL");
            }

            Integer advisorIdx = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'user_groups' AND index_name = 'idx_user_groups_advisor'",
                    Integer.class);
            if (advisorIdx != null && advisorIdx == 0) {
                jdbcTemplate.execute("CREATE INDEX idx_user_groups_advisor ON user_groups(advisor_id)");
            }

            Integer advisorFk = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.table_constraints WHERE table_schema = DATABASE() AND table_name = 'user_groups' AND constraint_name = 'fk_user_groups_advisor'",
                    Integer.class);
            if (advisorFk != null && advisorFk == 0) {
                jdbcTemplate.execute(
                        "ALTER TABLE user_groups ADD CONSTRAINT fk_user_groups_advisor FOREIGN KEY (advisor_id) REFERENCES users(id)");
            }
        } catch (Exception ignored) {
            // Best effort migration.
        }
    }
}
