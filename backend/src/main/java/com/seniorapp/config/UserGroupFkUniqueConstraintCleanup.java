package com.seniorapp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Eski şemada {@code team_leader_id} / {@code coordinator_id} üzerinde UNIQUE + FOREIGN KEY birlikte
 * olduğunda MySQL, UNIQUE indeksi kullanan FK yüzünden {@code DROP INDEX} işlemine izin vermez.
 * Bu sınıf FK'yi düşürür, UNIQUE'i kaldırır, sıradan indeks ekler ve FK'yi yeniden oluşturur.
 */
@Component
@DependsOn("entityManagerFactory")
public class UserGroupFkUniqueConstraintCleanup implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(UserGroupFkUniqueConstraintCleanup.class);

    /** Bazı kurulumlarda hata çıktısında görünen sabit UNIQUE ismi. */
    private static final String LEGACY_UK_BY_ERROR_MESSAGE = "UKly5w9q0vkwvjlsdoqugnebfeb";

    private final JdbcTemplate jdbc;

    public UserGroupFkUniqueConstraintCleanup(JdbcTemplate jdbcTemplate) {
        this.jdbc = jdbcTemplate;
    }

    @Override
    public void afterPropertiesSet() {
        boolean tableMissing = missingUserGroupsTable();
        if (tableMissing) {
            log.debug("user_groups table missing; skip FK/unique cleanup.");
            return;
        }
        try {
            dropForeignKeysOnColumn("team_leader_id");
            dropForeignKeysOnColumn("coordinator_id");

            tryDropNamedUnique(LEGACY_UK_BY_ERROR_MESSAGE);

            Set<String> uniqueIndexNames = new LinkedHashSet<>();
            try {
                uniqueIndexNames.addAll(findUniqueIndexesOnColumns("team_leader_id", "coordinator_id"));
            } catch (Exception e) {
                log.warn("STATISTICS-based unique index lookup failed: {}", e.getMessage());
            }
            uniqueIndexNames.addAll(findUniqueConstraintsFromTc("team_leader_id", "coordinator_id"));

            for (String indexName : uniqueIndexNames) {
                dropIndexIfPossible(indexName);
            }

            ensureBtreeIndex("idx_user_groups_team_leader", "team_leader_id");
            ensureBtreeIndex("idx_user_groups_coordinator", "coordinator_id");

            addForeignKeyIfMissing("fk_user_groups_team_leader", "team_leader_id", "users", "id");
            addForeignKeyIfMissing("fk_user_groups_coordinator", "coordinator_id", "users", "id");
        } catch (Exception e) {
            log.error("user_groups unique/FK normalization failed; you may need manual SQL: {}", e.getMessage(), e);
        }
    }

    private boolean missingUserGroupsTable() {
        try {
            Integer n = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'user_groups'",
                    Integer.class);
            return n == null || n == 0;
        } catch (Exception e) {
            log.warn("Could not check user_groups existence: {}", e.getMessage());
            return true;
        }
    }

    private void dropForeignKeysOnColumn(String column) {
        try {
            String sql =
                    "SELECT CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE "
                            + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_groups' "
                            + "AND COLUMN_NAME = ? AND REFERENCED_TABLE_NAME IS NOT NULL";
            List<String> names = jdbc.query(sql, (rs, rowNum) -> rs.getString(1), column);
            LinkedHashSet<String> unique = new LinkedHashSet<>(names);
            for (String fk : unique) {
                if (fk == null || fk.isBlank()) {
                    continue;
                }
                try {
                    jdbc.execute("ALTER TABLE user_groups DROP FOREIGN KEY `" + sanitize(fk) + "`");
                    log.info("Dropped foreign key {} on user_groups ({}).", fk, column);
                } catch (Exception e) {
                    log.warn("Could not DROP FOREIGN KEY {}: {}", fk, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("FK discovery for {} failed: {}", column, e.getMessage());
        }
    }

    private void tryDropNamedUnique(String indexName) {
        try {
            jdbc.execute("ALTER TABLE user_groups DROP INDEX `" + sanitize(indexName) + "`");
            log.info("Dropped unique index {} on user_groups.", indexName);
        } catch (Exception e) {
            log.debug("Named unique {} not dropped: {}", indexName, e.getMessage());
        }
    }

    private List<String> findUniqueIndexesOnColumns(String... columns) {
        if (columns.length == 0) {
            return List.of();
        }
        String inList = String.join(", ", java.util.Arrays.stream(columns).map(c -> "'" + sanitize(c) + "'").toList());
        String sql =
                "SELECT DISTINCT INDEX_NAME FROM information_schema.STATISTICS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_groups' "
                        + "AND COLUMN_NAME IN (" + inList + ") "
                        + "AND NON_UNIQUE = 0 AND INDEX_NAME <> 'PRIMARY'";
        return jdbc.query(sql, (rs, rowNum) -> rs.getString(1));
    }

    private List<String> findUniqueConstraintsFromTc(String... columns) {
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        for (String col : columns) {
            String s = sanitize(col);
            String sql =
                    "SELECT DISTINCT kcu.CONSTRAINT_NAME "
                            + "FROM information_schema.KEY_COLUMN_USAGE kcu "
                            + "JOIN information_schema.TABLE_CONSTRAINTS tc ON "
                            + "  kcu.CONSTRAINT_NAME = tc.CONSTRAINT_NAME "
                            + "  AND kcu.TABLE_SCHEMA = tc.TABLE_SCHEMA "
                            + "  AND kcu.TABLE_NAME = tc.TABLE_NAME "
                            + "WHERE kcu.TABLE_SCHEMA = DATABASE() "
                            + "  AND kcu.TABLE_NAME = 'user_groups' "
                            + "  AND tc.CONSTRAINT_TYPE = 'UNIQUE' "
                            + "  AND kcu.COLUMN_NAME = '" + s + "'";
            try {
                out.addAll(jdbc.query(sql, (rs, rowNum) -> rs.getString(1)));
            } catch (Exception e) {
                log.debug("TC unique lookup for {} failed: {}", col, e.getMessage());
            }
        }
        return out;
    }

    private void dropIndexIfPossible(String indexName) {
        if (indexName == null || indexName.isBlank() || "PRIMARY".equalsIgnoreCase(indexName)) {
            return;
        }
        try {
            jdbc.execute("ALTER TABLE user_groups DROP INDEX `" + sanitize(indexName) + "`");
            log.info("Dropped index {} on user_groups.", indexName);
        } catch (Exception e) {
            log.debug("DROP INDEX {} skipped: {}", indexName, e.getMessage());
        }
    }

    private void ensureBtreeIndex(String indexName, String column) {
        try {
            jdbc.execute("CREATE INDEX `" + sanitize(indexName) + "` ON user_groups (`" + sanitize(column) + "`)");
            log.info("Created index {} on user_groups({}).", indexName, column);
        } catch (Exception e) {
            log.debug("CREATE INDEX {} (may already exist): {}", indexName, e.getMessage());
        }
    }

    private void addForeignKeyIfMissing(String constraintName, String column, String refTable, String refCol) {
        try {
            jdbc.execute(
                    "ALTER TABLE user_groups ADD CONSTRAINT `"
                            + sanitize(constraintName)
                            + "` FOREIGN KEY (`"
                            + sanitize(column)
                            + "`) REFERENCES `"
                            + sanitize(refTable)
                            + "` (`"
                            + sanitize(refCol)
                            + "`)");
            log.info("Added foreign key {} on user_groups({}).", constraintName, column);
        } catch (Exception e) {
            log.debug("ADD FK {} (may already exist): {}", constraintName, e.getMessage());
        }
    }

    private static String sanitize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("`", "");
    }
}
