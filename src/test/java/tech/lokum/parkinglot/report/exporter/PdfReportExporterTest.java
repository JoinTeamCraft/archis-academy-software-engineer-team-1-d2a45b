package tech.lokum.parkinglot.report.exporter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tech.lokum.parkinglot.report.model.BookingTrendsReportData;
import tech.lokum.parkinglot.report.model.PaymentSummaryReportData;
import tech.lokum.parkinglot.report.model.ReportFormat;
import tech.lokum.parkinglot.report.model.UserActivityReportData;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PdfReportExporterTest {

    private PdfReportExporter exporter;

    @BeforeEach
    void setUp() {
        exporter = new PdfReportExporter();
    }

    @Test
    @DisplayName("Format should be PDF")
    void shouldReportPdfFormat() {
        assertThat(exporter.getFormat()).isEqualTo(ReportFormat.PDF);
    }

    @Test
    @DisplayName("exportUserActivity should generate valid non-empty PDF binary")
    void shouldGenerateUserActivityPdf() {
        UserActivityReportData.UserActivityItem item = new UserActivityReportData.UserActivityItem(
                1L, "diana_p", "diana@example.com", "Diana Prince", "ADMIN", true, 5L, BigDecimal.valueOf(250.00), Instant.now()
        );
        UserActivityReportData data = new UserActivityReportData(
                Instant.now().minusSeconds(86400), Instant.now(), 1L, 1L, 0L, Map.of("ADMIN", 1L), List.of(item)
        );

        byte[] pdf = exporter.exportUserActivity(data);

        assertThat(pdf).isNotNull().isNotEmpty();
        // Check standard PDF file header
        String header = new String(pdf, 0, 5, StandardCharsets.US_ASCII);
        assertThat(header).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("exportBookingTrends should generate valid non-empty PDF binary")
    void shouldGenerateBookingTrendsPdf() {
        BookingTrendsReportData.BookingTrendItem item = new BookingTrendsReportData.BookingTrendItem(
                12L, "barry@example.com", "Barry Allen", "Speed Lot", "S-01", "FLASH-1", Instant.now(), Instant.now().plusSeconds(7200), BigDecimal.valueOf(30.00), "COMPLETED"
        );
        BookingTrendsReportData data = new BookingTrendsReportData(
                Instant.now().minusSeconds(86400), Instant.now(), 1L, Map.of("COMPLETED", 1L), Map.of("Speed Lot", 1L), Map.of("2026-09-29", 1L), BigDecimal.valueOf(30.00), 2.0, List.of(item)
        );

        byte[] pdf = exporter.exportBookingTrends(data);

        assertThat(pdf).isNotNull().isNotEmpty();
        String header = new String(pdf, 0, 5, StandardCharsets.US_ASCII);
        assertThat(header).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("exportPaymentSummary should generate valid non-empty PDF binary")
    void shouldGeneratePaymentSummaryPdf() {
        PaymentSummaryReportData.PaymentSummaryItem item = new PaymentSummaryReportData.PaymentSummaryItem(
                303L, "TX-554433", 12L, "barry@example.com", BigDecimal.valueOf(30.00), "USD", "DEBIT_CARD", "SUCCESS", Instant.now()
        );
        PaymentSummaryReportData data = new PaymentSummaryReportData(
                Instant.now().minusSeconds(86400), Instant.now(), 1L, BigDecimal.valueOf(30.00), Map.of("SUCCESS", 1L), Map.of("DEBIT_CARD", BigDecimal.valueOf(30.00)), Map.of("2026-09", BigDecimal.valueOf(30.00)), List.of(item)
        );

        byte[] pdf = exporter.exportPaymentSummary(data);

        assertThat(pdf).isNotNull().isNotEmpty();
        String header = new String(pdf, 0, 5, StandardCharsets.US_ASCII);
        assertThat(header).isEqualTo("%PDF-");
    }
}
