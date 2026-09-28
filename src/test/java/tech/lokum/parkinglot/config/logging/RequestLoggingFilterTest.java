package tech.lokum.parkinglot.config.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static tech.lokum.parkinglot.config.logging.RequestLoggingFilter.REQUEST_ID_HEADER;
import static tech.lokum.parkinglot.config.logging.RequestLoggingFilter.REQUEST_ID_MDC_KEY;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestLoggingFilterTest {

    private final RequestLoggingFilter filter = new RequestLoggingFilter();
    private final Logger logger = (Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @BeforeEach
    void captureLogs() {
        logger.setLevel(Level.DEBUG);
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void stopCapturing() {
        logger.detachAppender(appender);
        logger.setLevel(null);
    }

    @Test
    void logsMethodPathStatusAndDurationAtInfo() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/lots");
        request.setQueryString("token=abc");

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> { });

        ILoggingEvent event = appender.list.getFirst();
        assertThat(event.getLevel()).isEqualTo(Level.INFO);
        assertThat(event.getFormattedMessage()).matches("GET /api/lots -> 200 \\(\\d+ ms\\)");
    }

    @Test
    void clientErrorsAreWarnAndServerErrorsAreError() throws Exception {
        filter.doFilter(new MockHttpServletRequest("POST", "/api/auth/login"), new MockHttpServletResponse(),
                (req, res) -> ((MockHttpServletResponse) res).setStatus(401));
        filter.doFilter(new MockHttpServletRequest("GET", "/api/lots"), new MockHttpServletResponse(),
                (req, res) -> ((MockHttpServletResponse) res).setStatus(503));

        assertThat(appender.list).extracting(ILoggingEvent::getLevel).containsExactly(Level.WARN, Level.ERROR);
    }

    @Test
    void unhandledExceptionIsLoggedAs500AndRethrown() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/lots");

        assertThatThrownBy(() -> filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class);

        ILoggingEvent event = appender.list.getFirst();
        assertThat(event.getLevel()).isEqualTo(Level.ERROR);
        assertThat(event.getFormattedMessage()).startsWith("GET /api/lots -> 500");
    }

    @Test
    void healthChecksAreDebug() throws Exception {
        filter.doFilter(new MockHttpServletRequest("GET", "/actuator/health"), new MockHttpServletResponse(),
                (req, res) -> { });

        assertThat(appender.list.getFirst().getLevel()).isEqualTo(Level.DEBUG);
    }

    @Test
    void requestIdIsInMdcDuringTheRequestAndEchoedBack() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/lots");
        request.addHeader(REQUEST_ID_HEADER, "client-id-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> idSeenByHandler = new AtomicReference<>();

        filter.doFilter(request, response, (req, res) -> idSeenByHandler.set(MDC.get(REQUEST_ID_MDC_KEY)));

        assertThat(idSeenByHandler.get()).isEqualTo("client-id-123");
        assertThat(response.getHeader(REQUEST_ID_HEADER)).isEqualTo("client-id-123");
        assertThat(appender.list.getFirst().getMDCPropertyMap()).containsEntry(REQUEST_ID_MDC_KEY, "client-id-123");
        assertThat(MDC.get(REQUEST_ID_MDC_KEY)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc\nFAKE LOG LINE", "has spaces", "<script>"})
    void unsafeRequestIdIsReplaced(String header) {
        assertThat(RequestLoggingFilter.resolveRequestId(header))
                .isNotEqualTo(header)
                .matches("[0-9a-f-]{36}");
    }

    @Test
    void overlongRequestIdIsReplaced() {
        assertThat(RequestLoggingFilter.resolveRequestId("a".repeat(65))).hasSize(36);
    }
}
