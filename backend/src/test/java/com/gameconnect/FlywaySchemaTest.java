package com.gameconnect;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Verifies that Flyway applies the full V1 schema against a real PostgreSQL
 * container, so later phases can rely on the migrations being correct.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class FlywaySchemaTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayAppliesInitialMigration() {
        flyway.migrate();
        var applied = flyway.info().applied().length;
        assertThat(applied).as("at least the V1 migration is applied").isGreaterThanOrEqualTo(1);
    }

    @Test
    void coreTablesExist() {
        var tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
                String.class);
        assertThat(tables).contains(
                "app_user", "game", "join_request", "match_participant", "player_rating");
    }

    @Test
    void positionColumnExistsOnAppUser() {
        var columns = jdbcTemplate.queryForList(
                """
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = 'app_user'
                """,
                String.class);
        assertThat(columns).contains("position");
    }

    @Test
    void uniqueAndCheckConstraintsArePresent() {
        List<String> constraints = jdbcTemplate.queryForList(
                """
                SELECT tc.constraint_name
                FROM information_schema.table_constraints tc
                JOIN information_schema.constraint_column_usage ccu
                  ON ccu.constraint_name = tc.constraint_name
                WHERE tc.table_schema = 'public'
                """,
                String.class);
        assertThat(constraints).contains(
                "uq_app_user_email",
                "uq_req_game_user",
                "uq_participant",
                "uq_rating",
                "ck_game_status",
                "ck_rating_score",
                "ck_rating_self",
                "ck_app_user_position");
    }
}
