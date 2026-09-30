package tech.lokum.parkinglot.migration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that Flyway migrations apply cleanly and that Hibernate can validate
 * the resulting schema against entity mappings.
 *
 * <p>Uses the {@code flyway-test} profile which enables Flyway with H2 in
 * PostgreSQL compatibility mode and sets {@code ddl-auto=validate} so Hibernate
 * confirms the migration-created schema matches entity mappings.</p>
 *
 * <p>If the application context loads successfully, it means:
 * <ol>
 *   <li>Flyway ran all migrations without errors.</li>
 *   <li>Hibernate {@code ddl-auto=validate} passed.</li>
 * </ol>
 */
@SpringBootTest
@ActiveProfiles("flyway-test")
class FlywayMigrationTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private DataSource dataSource;

    @Test
    void contextLoads_withFlywayMigrations() {
        // If we get here, Flyway migrations applied and Hibernate validation passed.
        assertNotNull(flyway, "Flyway bean should be auto-configured");
    }

    @Test
    void allMigrations_areApplied() {
        var info = flyway.info();
        var applied = info.applied();

        assertTrue(applied.length > 0, "At least one migration should have been applied");

        // V1 should be in the applied list
        boolean hasV1 = false;
        for (var migration : applied) {
            if (migration.getVersion() != null
                    && migration.getVersion().toString().equals("1")) {
                hasV1 = true;
                assertEquals("initial schema",
                        migration.getDescription().toLowerCase(),
                        "V1 migration description should be 'initial schema'");
            }
        }
        assertTrue(hasV1, "V1__initial_schema migration must be applied");
    }

    @Test
    void noMigrations_arePending() {
        var pending = flyway.info().pending();
        assertEquals(0, pending.length,
                "No migrations should be pending after startup");
    }

    @Test
    void schemaTables_areCreatedInDatabase() throws Exception {
        Set<String> tables = new HashSet<>();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC'")) {
            while (rs.next()) {
                tables.add(rs.getString(1).toLowerCase());
            }
        }

        assertTrue(tables.contains("users"), "Table 'users' should exist");
        assertTrue(tables.contains("parking_lots"), "Table 'parking_lots' should exist");
        assertTrue(tables.contains("parking_spots"), "Table 'parking_spots' should exist");
        assertTrue(tables.contains("vehicles"), "Table 'vehicles' should exist");
        assertTrue(tables.contains("reservations"), "Table 'reservations' should exist");
        assertTrue(tables.contains("payments"), "Table 'payments' should exist");
        assertTrue(tables.contains("report_metadata"), "Table 'report_metadata' should exist");
        assertTrue(tables.contains("flyway_schema_history"), "Table 'flyway_schema_history' should exist");
    }
}
