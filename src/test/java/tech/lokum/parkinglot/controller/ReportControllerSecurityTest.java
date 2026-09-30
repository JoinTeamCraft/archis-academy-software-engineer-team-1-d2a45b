package tech.lokum.parkinglot.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tech.lokum.parkinglot.entity.Role;
import tech.lokum.parkinglot.report.dto.ReportRequest;
import tech.lokum.parkinglot.report.dto.ReportResponse;
import tech.lokum.parkinglot.report.service.ReportService;
import tech.lokum.parkinglot.security.JwtService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReportControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private ReportService reportService;

    private String createToken(String email, Role role) {
        return "Bearer " + jwtService.generateToken(email, role);
    }

    @Nested
    @DisplayName("GET /api/reports - Role Restrictions")
    class ReportEndpointSecurityTests {

        @Test
        @DisplayName("GET /api/reports without token should fail with 401 Unauthorized")
        void unauthenticatedRequestFailsWith401() throws Exception {
            mockMvc.perform(get("/api/reports")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "reportType": "monthlyRevenue",
                                      "startDate": "2025-01-01",
                                      "endDate": "2025-01-31"
                                    }
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("GET /api/reports with CUSTOMER role should fail with 403 Forbidden")
        void customerRequestFailsWith403() throws Exception {
            mockMvc.perform(get("/api/reports")
                            .header("Authorization", createToken("customer@test.com", Role.CUSTOMER))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "reportType": "monthlyRevenue",
                                      "startDate": "2025-01-01",
                                      "endDate": "2025-01-31"
                                    }
                                    """))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET /api/reports with USER role should fail with 403 Forbidden")
        void userRoleRequestFailsWith403() throws Exception {
            mockMvc.perform(get("/api/reports")
                            .header("Authorization", createToken("user@test.com", Role.USER))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "reportType": "monthlyRevenue",
                                      "startDate": "2025-01-01",
                                      "endDate": "2025-01-31"
                                    }
                                    """))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("GET /api/reports with ADMIN role should succeed with 200 OK")
        void adminRequestSucceeds() throws Exception {
            ReportResponse response = new ReportResponse(123L, "generated", "/api/reports/download/123");
            when(reportService.requestReport(any(ReportRequest.class), any())).thenReturn(response);

            mockMvc.perform(get("/api/reports")
                            .header("Authorization", createToken("admin@test.com", Role.ADMIN))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "reportType": "monthlyRevenue",
                                      "startDate": "2025-01-01",
                                      "endDate": "2025-01-31"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.reportId").value(123))
                    .andExpect(jsonPath("$.status").value("generated"))
                    .andExpect(jsonPath("$.downloadLink").value("/api/reports/download/123"));
        }

        @Test
        @DisplayName("GET /api/reports with MANAGER role should succeed with 200 OK")
        void managerRequestSucceeds() throws Exception {
            ReportResponse response = new ReportResponse(124L, "generated", "/api/reports/download/124");
            when(reportService.requestReport(any(ReportRequest.class), any())).thenReturn(response);

            mockMvc.perform(get("/api/reports")
                            .header("Authorization", createToken("manager@test.com", Role.MANAGER))
                            .param("reportType", "bookingTrends"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.reportId").value(124));
        }

        @Test
        @DisplayName("GET /api/reports with OPERATOR role should succeed with 200 OK")
        void operatorRequestSucceeds() throws Exception {
            ReportResponse response = new ReportResponse(125L, "generated", "/api/reports/download/125");
            when(reportService.requestReport(any(ReportRequest.class), any())).thenReturn(response);

            mockMvc.perform(get("/api/reports")
                            .header("Authorization", createToken("operator@test.com", Role.OPERATOR))
                            .param("reportType", "userActivity"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.reportId").value(125));
        }
    }
}
