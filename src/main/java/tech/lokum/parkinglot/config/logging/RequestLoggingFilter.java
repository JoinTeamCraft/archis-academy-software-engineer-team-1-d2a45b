package tech.lokum.parkinglot.config.logging;

import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.event.Level;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Logs one line per API call ({@code GET /api/lots -> 200 (12 ms)}) and tags every log line written
 * while handling the request with a request id.
 *
 * <p>The id comes from the {@code X-Request-Id} header when the caller sends a safe one, otherwise a
 * new one is generated. It is returned in the response header so a client can quote it in a bug
 * report. Only the method and path are logged: headers, query strings and bodies can carry tokens
 * or passwords.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String REQUEST_ID_MDC_KEY = "requestId";

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    // Rejects anything that could forge extra log lines or flood the log.
    private static final Pattern SAFE_REQUEST_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String requestId = resolveRequestId(request.getHeader(REQUEST_ID_HEADER));
        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);
        long start = System.nanoTime();
        boolean failed = false;
        try {
            chain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException e) {
            // Only exceptions nothing downstream handled get here, and the container turns those into a
            // 500 after we return. The exception itself is logged there.
            failed = true;
            throw e;
        } finally {
            if (!failed && request.isAsyncStarted()) {
                // The response is finished later on another thread, so log once it completes.
                request.getAsyncContext()
                        .addListener(new AsyncCompletionLogger(request, response, requestId, start));
            } else {
                int status = failed ? HttpServletResponse.SC_INTERNAL_SERVER_ERROR : response.getStatus();
                logRequest(request, status, start);
            }
            MDC.remove(REQUEST_ID_MDC_KEY);
        }
    }

    private static void logRequest(HttpServletRequest request, int status, long startNanos) {
        long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
        String path = pathOnly(request.getRequestURI());
        log.atLevel(levelFor(path, request.getContextPath(), status))
                .log("{} {} -> {} ({} ms)", request.getMethod(), path, status, durationMs);
    }

    static String resolveRequestId(String header) {
        if (header != null && SAFE_REQUEST_ID.matcher(header).matches()) {
            return header;
        }
        return UUID.randomUUID().toString();
    }

    // getRequestURI() excludes the query string by spec; cut at '?' anyway so it can never be logged.
    static String pathOnly(String uri) {
        if (uri == null) {
            return "";
        }
        int query = uri.indexOf('?');
        return query < 0 ? uri : uri.substring(0, query);
    }

    static Level levelFor(String path, String contextPath, int status) {
        if (status >= 500) {
            return Level.ERROR;
        }
        if (status >= 400) {
            return Level.WARN;
        }
        // Health checks hit the app every few seconds; keep them out of the INFO log.
        String appPath = contextPath != null && path.startsWith(contextPath)
                ? path.substring(contextPath.length()) : path;
        return appPath.startsWith("/actuator") ? Level.DEBUG : Level.INFO;
    }

    /** Logs an async request when its response is complete, with the request id put back in the MDC. */
    private record AsyncCompletionLogger(HttpServletRequest request, HttpServletResponse response, String requestId,
            long startNanos) implements AsyncListener {

        @Override
        public void onComplete(AsyncEvent event) {
            MDC.put(REQUEST_ID_MDC_KEY, requestId);
            try {
                logRequest(request, response.getStatus(), startNanos);
            } finally {
                MDC.remove(REQUEST_ID_MDC_KEY);
            }
        }

        @Override
        public void onStartAsync(AsyncEvent event) {
            // A new async cycle drops its listeners; stay registered until the response completes.
            event.getAsyncContext().addListener(this);
        }

        // The container completes the request after a timeout or error, so onComplete still logs it.
        @Override
        public void onTimeout(AsyncEvent event) {
        }

        @Override
        public void onError(AsyncEvent event) {
        }
    }
}
