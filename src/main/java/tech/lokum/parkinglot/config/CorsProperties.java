package tech.lokum.parkinglot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for Cross-Origin Resource Sharing (CORS).
 * Enables environment-specific allowed origins, HTTP methods, headers, and credential policies.
 */
@Component
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    /**
     * Allowed origin URLs for cross-origin requests.
     * Default includes common local development frontends.
     */
    private List<String> allowedOrigins = new ArrayList<>(List.of(
            "http://localhost:3000",
            "http://localhost:5173",
            "http://localhost:8080"
    ));

    /**
     * Allowed HTTP request methods.
     */
    private List<String> allowedMethods = new ArrayList<>(List.of(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"
    ));

    /**
     * Allowed HTTP request headers.
     */
    private List<String> allowedHeaders = new ArrayList<>(List.of(
            "Authorization",
            "Content-Type",
            "Accept",
            "Origin",
            "X-Requested-With",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers",
            "Stripe-Signature"
    ));

    /**
     * Response headers exposed to client-side scripts.
     */
    private List<String> exposedHeaders = new ArrayList<>(List.of(
            "Authorization",
            "Content-Disposition",
            "X-Total-Count",
            "X-Total-Pages",
            "X-Current-Page",
            "X-Page-Size"
    ));

    /**
     * Whether user credentials (cookies, authorization headers) are supported.
     */
    private boolean allowCredentials = true;

    /**
     * Preflight cache duration in seconds.
     */
    private long maxAge = 3600L;

    public CorsConfiguration toCorsConfiguration() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(allowedMethods);
        config.setAllowedHeaders(allowedHeaders);
        config.setExposedHeaders(exposedHeaders);
        config.setAllowCredentials(allowCredentials);
        config.setMaxAge(maxAge);
        return config;
    }

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public List<String> getAllowedMethods() {
        return allowedMethods;
    }

    public void setAllowedMethods(List<String> allowedMethods) {
        this.allowedMethods = allowedMethods;
    }

    public List<String> getAllowedHeaders() {
        return allowedHeaders;
    }

    public void setAllowedHeaders(List<String> allowedHeaders) {
        this.allowedHeaders = allowedHeaders;
    }

    public List<String> getExposedHeaders() {
        return exposedHeaders;
    }

    public void setExposedHeaders(List<String> exposedHeaders) {
        this.exposedHeaders = exposedHeaders;
    }

    public boolean isAllowCredentials() {
        return allowCredentials;
    }

    public void setAllowCredentials(boolean allowCredentials) {
        this.allowCredentials = allowCredentials;
    }

    public long getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(long maxAge) {
        this.maxAge = maxAge;
    }
}

