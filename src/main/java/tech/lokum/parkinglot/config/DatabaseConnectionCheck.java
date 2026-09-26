package tech.lokum.parkinglot.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Fails startup early with a clear message if the database is unreachable. */
@Component
public class DatabaseConnectionCheck implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConnectionCheck.class);

    private final DataSource dataSource;

    public DatabaseConnectionCheck(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Hikari validates the connection before handing it out, so getting one is the check.
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData meta = connection.getMetaData();
            log.info("Connected to {} {} at {}",
                    meta.getDatabaseProductName(), meta.getDatabaseProductVersion(), meta.getURL());
        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Cannot connect to the database. Check DB_URL, DB_USERNAME and DB_PASSWORD, "
                            + "and that Postgres is running (docker compose up -d postgres).", e);
        }
    }
}
