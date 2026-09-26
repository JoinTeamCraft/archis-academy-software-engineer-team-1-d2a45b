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
    private static final int VALIDATION_TIMEOUT_SECONDS = 5;

    private final DataSource dataSource;

    public DatabaseConnectionCheck(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            if (!connection.isValid(VALIDATION_TIMEOUT_SECONDS)) {
                throw new IllegalStateException("Database connection is not valid");
            }
            DatabaseMetaData meta = connection.getMetaData();
            log.info("Connected to {} {} at {}",
                    meta.getDatabaseProductName(), meta.getDatabaseProductVersion(), meta.getURL());
        }
    }
}
