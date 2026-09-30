package tech.lokum.parkinglot.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests verifying that OpenAPI 3 documentation and Swagger UI
 * endpoints are hosted publicly, properly configured, and describe all system endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiDocumentationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api-docs should return OpenAPI 3.0 JSON specification without authentication")
    void getApiDocsJson_shouldReturnOpenApiSpec() throws Exception {
        mockMvc.perform(get("/api-docs")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.openapi").value(containsString("3.")))
                .andExpect(jsonPath("$.info.title").value("Parking Lot Management System API"))
                .andExpect(jsonPath("$.info.version").value("1.0.0"))
                .andExpect(jsonPath("$.tags[*].name").value(hasItem("Authentication")))
                .andExpect(jsonPath("$.tags[*].name").value(hasItem("Users")))
                .andExpect(jsonPath("$.tags[*].name").value(hasItem("Parking Lots")))
                .andExpect(jsonPath("$.tags[*].name").value(hasItem("Reservations")))
                .andExpect(jsonPath("$.tags[*].name").value(hasItem("Payments")))
                .andExpect(jsonPath("$.tags[*].name").value(hasItem("Reports")))
                .andExpect(jsonPath("$.tags[*].name").value(hasItem("Feedback")))
                .andExpect(jsonPath("$.tags[*].name").value(hasItem("System Status")))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth").exists())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.paths['/api/auth/login']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/register']").exists())
                .andExpect(jsonPath("$.paths['/api/parking-lots']").exists())
                .andExpect(jsonPath("$.paths['/api/reservations']").exists())
                .andExpect(jsonPath("$.paths['/api/payments']").exists())
                .andExpect(jsonPath("$.paths['/api/reports']").exists())
                .andExpect(jsonPath("$.paths['/api/feedback']").exists())
                .andExpect(jsonPath("$.paths['/api/users']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/status']").exists());
    }

    @Test
    @DisplayName("GET /v3/api-docs should forward to /api-docs")
    void getV3ApiDocs_shouldForwardToApiDocs() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/api-docs"));
    }

    @Test
    @DisplayName("GET /swagger-ui.html should be accessible publicly without authentication")
    void getSwaggerUi_shouldBeAccessible() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("GET /docs should redirect to Swagger UI")
    void getDocs_shouldRedirectToSwaggerUi() throws Exception {
        mockMvc.perform(get("/docs"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/swagger-ui.html"));
    }

    @Test
    @DisplayName("GET /documentation should redirect to Swagger UI")
    void getDocumentation_shouldRedirectToSwaggerUi() throws Exception {
        mockMvc.perform(get("/documentation"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/swagger-ui.html"));
    }
}
