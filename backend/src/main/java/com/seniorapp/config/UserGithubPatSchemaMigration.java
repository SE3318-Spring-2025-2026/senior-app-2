package com.seniorapp.config;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class UserGithubPatSchemaMigration {
    public UserGithubPatSchemaMigration(JdbcTemplate jdbcTemplate) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'github_pat_encrypted'",
                    Integer.class);
            if (count != null && count == 0) {
                jdbcTemplate.execute("ALTER TABLE users ADD COLUMN github_pat_encrypted VARCHAR(1024) NULL");
            }
        } catch (Exception ignored) {
            // Best effort migration; do not block startup on schema inspection issues.
        }
    }
}
