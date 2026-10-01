package tech.lokum.parkinglot.config.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * logback-spring.xml sets root to INFO, but Spring Boot applies logging.level.* after loading it, so
 * LOG_LEVEL and friends still win.
 */
@SpringBootTest(properties = {
    "logging.level.root=WARN",
    "logging.level.tech.lokum.parkinglot=DEBUG",
})
class LogLevelConfigurationTest {

    // Levels are global to the JVM; put them back so other tests, which may reuse a cached context, see the defaults.
    @AfterEach
    void restoreDefaultLevels() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        context.getLogger(Logger.ROOT_LOGGER_NAME).setLevel(Level.INFO);
        context.getLogger("tech.lokum.parkinglot").setLevel(null);
    }

    @Test
    void configuredLevelsOverrideTheLogbackFile() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        assertThat(context.getLogger(Logger.ROOT_LOGGER_NAME).getLevel()).isEqualTo(Level.WARN);
        assertThat(context.getLogger("tech.lokum.parkinglot").getLevel()).isEqualTo(Level.DEBUG);
    }
}
