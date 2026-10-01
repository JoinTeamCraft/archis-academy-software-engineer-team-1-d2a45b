package tech.lokum.parkinglot.config.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static tech.lokum.parkinglot.config.logging.RequestLoggingFilter.REQUEST_ID_HEADER;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/** Checks logback-spring.xml end to end: masking, request ids and the rolling file appender. */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class LoggingConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void secretsAreMaskedInTheLogOutput(CapturedOutput output) {
        LoggerFactory.getLogger(LoggingConfigurationTest.class)
                .info("login attempt {\"email\":\"a@b.io\",\"password\":\"{}\"}", "hunter2");

        assertThat(output).contains("\"password\":\"****\"").doesNotContain("hunter2");
    }

    @Test
    void apiCallsAreLoggedWithTheirRequestId(CapturedOutput output) throws Exception {
        mockMvc.perform(get("/api/does-not-exist").header(REQUEST_ID_HEADER, "test-req-1"))
                .andExpect(header().string(REQUEST_ID_HEADER, "test-req-1"));

        assertThat(output).containsPattern("WARN .*\\[test-req-1] .*GET /api/does-not-exist -> 404 \\(\\d+ ms\\)");
    }

    @Test
    void linesOutsideARequestHaveNoEmptyRequestIdBrackets(CapturedOutput output) {
        LoggerFactory.getLogger(LoggingConfigurationTest.class).info("outside any request");

        assertThat(output).contains("outside any request").doesNotContain("[] ");
    }

    @Test
    void logsAreWrittenToARotatingFile() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger root = context.getLogger(Logger.ROOT_LOGGER_NAME);

        assertThat(root.getAppender("FILE")).isInstanceOfSatisfying(RollingFileAppender.class, file -> {
            assertThat(file.getFile()).endsWith("parking-lot.log");
            assertThat(file.getRollingPolicy()).isInstanceOf(SizeAndTimeBasedRollingPolicy.class);
        });
    }
}
