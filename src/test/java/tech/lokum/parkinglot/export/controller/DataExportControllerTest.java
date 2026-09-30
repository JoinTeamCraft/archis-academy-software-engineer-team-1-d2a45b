package tech.lokum.parkinglot.export.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tech.lokum.parkinglot.entity.Role;
import tech.lokum.parkinglot.export.dto.DataExportRequest;
import tech.lokum.parkinglot.export.dto.ExportedFile;
import tech.lokum.parkinglot.export.service.DataExportService;
import tech.lokum.parkinglot.security.JwtService;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DataExportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private DataExportService dataExportService;

    private String createToken(String email, Role role) {
        return "Bearer " + jwtService.generateToken(email, role);
    }

    @Test
    @DisplayName("GET /api/data/export with JSON request payload should export CSV file for ADMIN")
    void exportData_withJsonBody_asAdmin_shouldDownloadFile() throws Exception {
        byte[] csvBytes = "id,user_id,status\n1,10,CONFIRMED\n".getBytes(StandardCharsets.UTF_8);
        ExportedFile file = new ExportedFile("bookings_export_20260930_120000.csv", "text/csv; charset=UTF-8", csvBytes, 1);

        when(dataExportService.exportData(any(DataExportRequest.class))).thenReturn(file);

        String jsonPayload = """
                {
                  "dataType": "bookings",
                  "format": "CSV"
                }
                """;

        mockMvc.perform(get("/api/data/export")
                        .header(HttpHeaders.AUTHORIZATION, createToken("admin@example.com", Role.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment; filename=\"bookings_export_")))
                .andExpect(header().string("X-Export-Records", "1"))
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string(containsString("id,user_id,status")));
    }

    @Test
    @DisplayName("GET /api/data/export with query parameters should export JSON file for MANAGER")
    void exportData_withQueryParams_asManager_shouldDownloadJson() throws Exception {
        byte[] jsonBytes = "[{\"id\":1,\"email\":\"user@example.com\"}]".getBytes(StandardCharsets.UTF_8);
        ExportedFile file = new ExportedFile("users_export_20260930_120000.json", "application/json; charset=UTF-8", jsonBytes, 1);

        when(dataExportService.exportData(any(DataExportRequest.class))).thenReturn(file);

        mockMvc.perform(get("/api/data/export")
                        .header(HttpHeaders.AUTHORIZATION, createToken("manager@example.com", Role.MANAGER))
                        .param("dataType", "users")
                        .param("format", "JSON"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("attachment; filename=\"users_export_")))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string(containsString("\"email\":\"user@example.com\"")));
    }

    @Test
    @DisplayName("POST /api/data/export should support POST alternative with request body for OPERATOR")
    void exportDataPost_asOperator_shouldDownloadFile() throws Exception {
        byte[] csvBytes = "id,amount,currency\n50,15.00,USD\n".getBytes(StandardCharsets.UTF_8);
        ExportedFile file = new ExportedFile("payments_export_20260930_120000.csv", "text/csv; charset=UTF-8", csvBytes, 1);

        when(dataExportService.exportData(any(DataExportRequest.class))).thenReturn(file);

        String jsonPayload = """
                {
                  "dataType": "payments",
                  "format": "CSV"
                }
                """;

        mockMvc.perform(post("/api/data/export")
                        .header(HttpHeaders.AUTHORIZATION, createToken("operator@example.com", Role.OPERATOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("payments_export_")))
                .andExpect(content().string(containsString("id,amount,currency")));
    }

    @Test
    @DisplayName("GET /api/data/export without authentication should return 401 Unauthorized")
    void exportData_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/data/export")
                        .param("dataType", "bookings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/data/export with CUSTOMER role should return 403 Forbidden")
    void exportData_asCustomer_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/data/export")
                        .header(HttpHeaders.AUTHORIZATION, createToken("customer@example.com", Role.CUSTOMER))
                        .param("dataType", "bookings"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/data/export with USER role should return 403 Forbidden")
    void exportData_asUser_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/data/export")
                        .header(HttpHeaders.AUTHORIZATION, createToken("user@example.com", Role.USER))
                        .param("dataType", "bookings"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/data/export with ADMIN role missing dataType should return 400 Bad Request")
    void exportData_missingDataType_shouldReturn400() throws Exception {
        mockMvc.perform(get("/api/data/export")
                        .header(HttpHeaders.AUTHORIZATION, createToken("admin@example.com", Role.ADMIN)))
                .andExpect(status().isBadRequest());
    }
}
