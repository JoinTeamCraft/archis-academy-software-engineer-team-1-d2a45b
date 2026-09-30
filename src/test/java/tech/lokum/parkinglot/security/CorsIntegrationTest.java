package tech.lokum.parkinglot.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests verifying Cross-Origin Resource Sharing (CORS) behavior.
 * Validates allowed origins, HTTP methods, headers, preflight OPTIONS requests,
 * credential policies, exposed headers, untrusted domain blocking, and non-browser clients.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CorsIntegrationTest {

    private static final String TRUSTED_ORIGIN_REACT = "http://localhost:3000";
    private static final String TRUSTED_ORIGIN_VITE = "http://localhost:5173";
    private static final String TRUSTED_ORIGIN_SWAGGER = "http://localhost:8080";
    private static final String UNTRUSTED_ORIGIN = "https://malicious-attacker.com";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Preflight OPTIONS request from trusted origin (localhost:3000) should return 200 OK with CORS headers")
    void preflight_trustedOrigin_shouldAllowAndReturnHeaders() throws Exception {
        mockMvc.perform(options("/api/parking-lots")
                        .header(HttpHeaders.ORIGIN, TRUSTED_ORIGIN_REACT)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.POST.name())
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, TRUSTED_ORIGIN_REACT))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("POST")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("GET")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("PUT")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("DELETE")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString("Authorization")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString("Content-Type")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600"));
    }

    @Test
    @DisplayName("Preflight OPTIONS request from trusted Vite origin (localhost:5173) should be permitted")
    void preflight_viteOrigin_shouldAllowAndReturnHeaders() throws Exception {
        mockMvc.perform(options("/api/reservations")
                        .header(HttpHeaders.ORIGIN, TRUSTED_ORIGIN_VITE)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.PUT.name())
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, TRUSTED_ORIGIN_VITE))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("PUT")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    @DisplayName("Simple GET cross-origin request from trusted origin should return Access-Control-Allow-Origin")
    void simpleGet_trustedOrigin_shouldIncludeCorsHeaders() throws Exception {
        mockMvc.perform(get("/api/parking-lots")
                        .header(HttpHeaders.ORIGIN, TRUSTED_ORIGIN_REACT))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, TRUSTED_ORIGIN_REACT))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, containsString("X-Total-Count")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, containsString("Content-Disposition")));
    }

    @Test
    @DisplayName("Preflight OPTIONS request from untrusted origin should be rejected with 403 Forbidden")
    void preflight_untrustedOrigin_shouldBeRejected() throws Exception {
        mockMvc.perform(options("/api/parking-lots")
                        .header(HttpHeaders.ORIGIN, UNTRUSTED_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.POST.name()))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @DisplayName("Simple request from untrusted origin should not receive Access-Control-Allow-Origin header")
    void simpleGet_untrustedOrigin_shouldNotIncludeAllowOriginHeader() throws Exception {
        mockMvc.perform(get("/api/parking-lots")
                        .header(HttpHeaders.ORIGIN, UNTRUSTED_ORIGIN))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @DisplayName("Requests from native mobile apps or non-browser clients (no Origin header) should succeed normally")
    void nonBrowserClient_withoutOrigin_shouldSucceedWithoutCorsHeaders() throws Exception {
        mockMvc.perform(get("/api/parking-lots"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    @DisplayName("Payment webhook preflight or notification request from trusted origin should support custom headers")
    void preflight_paymentNotification_shouldAllowStripeHeaders() throws Exception {
        mockMvc.perform(options("/api/payments/webhook")
                        .header(HttpHeaders.ORIGIN, TRUSTED_ORIGIN_SWAGGER)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.POST.name())
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Stripe-Signature,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, TRUSTED_ORIGIN_SWAGGER))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString("Stripe-Signature")));
    }
}

