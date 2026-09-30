package tech.lokum.parkinglot.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tech.lokum.parkinglot.exception.GlobalExceptionHandler;
import tech.lokum.parkinglot.exception.ResourceNotFoundException;
import tech.lokum.parkinglot.report.dto.ReportRequest;
import tech.lokum.parkinglot.report.dto.ReportResponse;
import tech.lokum.parkinglot.report.entity.ReportMetadata;
import tech.lokum.parkinglot.report.model.GeneratedReport;
import tech.lokum.parkinglot.report.model.ReportFormat;
import tech.lokum.parkinglot.report.model.ReportType;
import tech.lokum.parkinglot.report.model.UserActivityReportData;
import tech.lokum.parkinglot.report.service.ReportService;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
@Import(GlobalExceptionHandler.class)
@WithMockUser(roles = "ADMIN")
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    @Test
    @DisplayName("GET /api/reports with JSON request payload should return reportId, status, and downloadLink")
    void shouldHandleGetReportWithJsonPayload() throws Exception {
        ReportResponse response = new ReportResponse(123L, "generated", "/api/reports/download/123");
        when(reportService.requestReport(any(ReportRequest.class), any())).thenReturn(response);

        mockMvc.perform(get("/api/reports")
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
    @DisplayName("GET /api/reports with query parameters should return reportId, status, and downloadLink")
    void shouldHandleGetReportWithQueryParams() throws Exception {
        ReportResponse response = new ReportResponse(124L, "generated", "/api/reports/download/124");
        when(reportService.requestReport(any(ReportRequest.class), any())).thenReturn(response);

        mockMvc.perform(get("/api/reports")
                        .param("reportType", "userActivity")
                        .param("startDate", "2025-01-01")
                        .param("endDate", "2025-01-31")
                        .param("format", "PDF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportId").value(124))
                .andExpect(jsonPath("$.status").value("generated"))
                .andExpect(jsonPath("$.downloadLink").value("/api/reports/download/124"));
    }

    @Test
    @DisplayName("POST /api/reports should also accept report request payload")
    void shouldHandlePostReportWithPayload() throws Exception {
        ReportResponse response = new ReportResponse(125L, "generated", "/api/reports/download/125");
        when(reportService.requestReport(any(ReportRequest.class), any())).thenReturn(response);

        mockMvc.perform(post("/api/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reportType": "bookingTrends",
                                  "startDate": "2025-01-01",
                                  "endDate": "2025-01-31",
                                  "format": "CSV"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reportId").value(125))
                .andExpect(jsonPath("$.status").value("generated"))
                .andExpect(jsonPath("$.downloadLink").value("/api/reports/download/125"));
    }

    @Test
    @DisplayName("GET /api/reports/download/{id} should stream stored report file")
    void shouldDownloadReportById() throws Exception {
        byte[] content = "test,csv,data\n1,2,3\n".getBytes(StandardCharsets.UTF_8);
        ReportMetadata metadata = new ReportMetadata("monthlyRevenue", "CSV", "generated", "2025-01-01", "2025-01-31", "monthly_revenue.csv", "text/csv; charset=UTF-8", "/api/reports/download/123", "admin", content);
        metadata.setId(123L);

        when(reportService.getReportMetadata(123L)).thenReturn(metadata);

        mockMvc.perform(get("/api/reports/download/123"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"monthly_revenue.csv\""))
                .andExpect(content().contentType("text/csv; charset=UTF-8"))
                .andExpect(content().bytes(content));
    }

    @Test
    @DisplayName("GET /api/reports/download/{id} should return 404 when report metadata not found")
    void shouldReturn404WhenReportNotFound() throws Exception {
        when(reportService.getReportMetadata(999L)).thenThrow(new ResourceNotFoundException("Report not found with id: 999"));

        mockMvc.perform(get("/api/reports/download/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Report not found with id: 999"));
    }

    @Test
    @DisplayName("GET /api/reports/user-activity should download user activity report in PDF format")
    void shouldDownloadUserActivityPdf() throws Exception {
        byte[] pdfContent = "%PDF-1.4 dummy binary".getBytes(StandardCharsets.UTF_8);
        GeneratedReport report = new GeneratedReport("user_activity.pdf", "application/pdf", pdfContent, ReportType.USER_ACTIVITY, ReportFormat.PDF, Instant.now());

        when(reportService.generateUserActivityReport(any(), any(), eq(ReportFormat.PDF))).thenReturn(report);

        mockMvc.perform(get("/api/reports/user-activity")
                        .param("format", "PDF"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"user_activity.pdf\""))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(pdfContent));
    }

    @Test
    @DisplayName("GET /api/reports/data/user-activity should return JSON representation")
    void shouldReturnUserActivityDataJson() throws Exception {
        UserActivityReportData data = new UserActivityReportData(
                Instant.now().minusSeconds(3600), Instant.now(), 5L, 4L, 1L, Map.of("CUSTOMER", 4L, "ADMIN", 1L), List.of()
        );

        when(reportService.fetchUserActivityData(any(), any())).thenReturn(data);

        mockMvc.perform(get("/api/reports/data/user-activity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(5))
                .andExpect(jsonPath("$.activeUsers").value(4));
    }
}
