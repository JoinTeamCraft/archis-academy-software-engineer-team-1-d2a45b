package tech.lokum.parkinglot.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tech.lokum.parkinglot.exception.GlobalExceptionHandler;
import tech.lokum.parkinglot.report.model.BookingTrendsReportData;
import tech.lokum.parkinglot.report.model.GeneratedReport;
import tech.lokum.parkinglot.report.model.PaymentSummaryReportData;
import tech.lokum.parkinglot.report.model.ReportFormat;
import tech.lokum.parkinglot.report.model.ReportType;
import tech.lokum.parkinglot.report.model.UserActivityReportData;
import tech.lokum.parkinglot.report.service.ReportService;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
@Import(GlobalExceptionHandler.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    @Test
    @DisplayName("GET /api/reports?type=USER_ACTIVITY&format=CSV should return 200 with CSV attachment")
    void shouldGenerateUserActivityReportViaGeneralEndpoint() throws Exception {
        byte[] csvContent = "User ID,Email\n1,alice@example.com\n".getBytes(StandardCharsets.UTF_8);
        GeneratedReport report = new GeneratedReport("user_activity_report.csv", "text/csv; charset=UTF-8", csvContent, ReportType.USER_ACTIVITY, ReportFormat.CSV, Instant.now());

        when(reportService.generateReport(eq(ReportType.USER_ACTIVITY), eq(ReportFormat.CSV), any(), any())).thenReturn(report);

        mockMvc.perform(get("/api/reports")
                        .param("type", "USER_ACTIVITY")
                        .param("format", "CSV"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"user_activity_report.csv\""))
                .andExpect(content().contentType("text/csv; charset=UTF-8"))
                .andExpect(content().bytes(csvContent));
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
    @DisplayName("GET /api/reports/booking-trends should download booking trends CSV")
    void shouldDownloadBookingTrendsCsv() throws Exception {
        byte[] csvContent = "Reservation ID,Status\n101,CONFIRMED\n".getBytes(StandardCharsets.UTF_8);
        GeneratedReport report = new GeneratedReport("booking_trends.csv", "text/csv; charset=UTF-8", csvContent, ReportType.BOOKING_TRENDS, ReportFormat.CSV, Instant.now());

        when(reportService.generateBookingTrendsReport(any(), any(), eq(ReportFormat.CSV))).thenReturn(report);

        mockMvc.perform(get("/api/reports/booking-trends")
                        .param("format", "CSV"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"booking_trends.csv\""))
                .andExpect(content().contentType("text/csv; charset=UTF-8"))
                .andExpect(content().bytes(csvContent));
    }

    @Test
    @DisplayName("GET /api/reports/payment-summary should download payment summary report")
    void shouldDownloadPaymentSummaryReport() throws Exception {
        byte[] csvContent = "Payment ID,Amount\n201,$50.00\n".getBytes(StandardCharsets.UTF_8);
        GeneratedReport report = new GeneratedReport("payment_summary.csv", "text/csv; charset=UTF-8", csvContent, ReportType.PAYMENT_SUMMARY, ReportFormat.CSV, Instant.now());

        when(reportService.generatePaymentSummaryReport(any(), any(), eq(ReportFormat.CSV))).thenReturn(report);

        mockMvc.perform(get("/api/reports/payment-summary")
                        .param("format", "CSV"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"payment_summary.csv\""))
                .andExpect(content().contentType("text/csv; charset=UTF-8"))
                .andExpect(content().bytes(csvContent));
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
                .andExpect(jsonPath("$.activeUsers").value(4))
                .andExpect(jsonPath("$.inactiveUsers").value(1))
                .andExpect(jsonPath("$.usersByRole.CUSTOMER").value(4));
    }

    @Test
    @DisplayName("GET /api/reports/data/booking-trends should return JSON representation")
    void shouldReturnBookingTrendsDataJson() throws Exception {
        BookingTrendsReportData data = new BookingTrendsReportData(
                Instant.now().minusSeconds(3600), Instant.now(), 12L, Map.of("CONFIRMED", 10L), Map.of("Lot A", 8L), Map.of("2026-09-29", 12L), BigDecimal.valueOf(360.00), 2.5, List.of()
        );

        when(reportService.fetchBookingTrendsData(any(), any())).thenReturn(data);

        mockMvc.perform(get("/api/reports/data/booking-trends"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBookings").value(12))
                .andExpect(jsonPath("$.totalRevenue").value(360.00))
                .andExpect(jsonPath("$.averageDurationHours").value(2.5));
    }

    @Test
    @DisplayName("GET /api/reports/data/payment-summary should return JSON representation")
    void shouldReturnPaymentSummaryDataJson() throws Exception {
        PaymentSummaryReportData data = new PaymentSummaryReportData(
                Instant.now().minusSeconds(3600), Instant.now(), 8L, BigDecimal.valueOf(250.00), Map.of("SUCCESS", 8L), Map.of("CREDIT_CARD", BigDecimal.valueOf(250.00)), Map.of("2026-09", BigDecimal.valueOf(250.00)), List.of()
        );

        when(reportService.fetchPaymentSummaryData(any(), any())).thenReturn(data);

        mockMvc.perform(get("/api/reports/data/payment-summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTransactions").value(8))
                .andExpect(jsonPath("$.totalRevenue").value(250.00))
                .andExpect(jsonPath("$.paymentsByStatus.SUCCESS").value(8));
    }
}
