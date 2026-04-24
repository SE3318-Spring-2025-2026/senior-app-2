package com.seniorapp.config;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class UserGroupSchemaMigration {

    public UserGroupSchemaMigration(JdbcTemplate jdbcTemplate) {
        Integer hasGroupId = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'group_id'",
                Integer.class
        );
        if (hasGroupId == null || hasGroupId == 0) {
            return;
        }

        jdbcTemplate.query(
                "SELECT constraint_name FROM information_schema.key_column_usage WHERE table_schema = DATABASE() AND table_name = 'users' AND column_name = 'group_id' AND referenced_table_name IS NOT NULL",
                (rs) -> {
                    String fkName = rs.getString("constraint_name");
                    jdbcTemplate.execute("ALTER TABLE users DROP FOREIGN KEY " + fkName);
                }
        );

        jdbcTemplate.execute("ALTER TABLE users DROP COLUMN group_id");
    }
}
