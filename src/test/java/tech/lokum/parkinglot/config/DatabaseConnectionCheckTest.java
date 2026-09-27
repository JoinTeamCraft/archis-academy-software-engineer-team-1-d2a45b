package tech.lokum.parkinglot.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

class DatabaseConnectionCheckTest {

    @Test
    void failsWithClearMessageWhenDatabaseIsUnreachable() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        SQLException cause = new SQLException("password authentication failed for user \"parking\"");
        when(dataSource.getConnection()).thenThrow(cause);

        assertThatThrownBy(() -> new DatabaseConnectionCheck(dataSource).run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot connect to the database")
                .hasCause(cause);
    }
}
