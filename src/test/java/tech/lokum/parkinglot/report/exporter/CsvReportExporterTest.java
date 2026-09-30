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

class CsvReportExporterTest {

    private CsvReportExporter exporter;

    @BeforeEach
    void setUp() {
        exporter = new CsvReportExporter();
    }

    @Test
    @DisplayName("Format should be CSV")
    void shouldReportCsvFormat() {
        assertThat(exporter.getFormat()).isEqualTo(ReportFormat.CSV);
    }

    @Test
    @DisplayName("exportUserActivity should produce valid CSV with escaped quotes and commas")
    void shouldExportUserActivityCsv() {
        UserActivityReportData.UserActivityItem item = new UserActivityReportData.UserActivityItem(
                1L, "doe,john", "john@example.com", "John \"The Driver\" Doe", "CUSTOMER", true, 3L, BigDecimal.valueOf(150.00), Instant.now()
        );
        UserActivityReportData data = new UserActivityReportData(
                Instant.now().minusSeconds(86400), Instant.now(), 1L, 1L, 0L, Map.of("CUSTOMER", 1L), List.of(item)
        );

        byte[] bytes = exporter.exportUserActivity(data);
        String csv = new String(bytes, StandardCharsets.UTF_8);

        assertThat(csv).contains("# USER ACTIVITY REPORT");
        assertThat(csv).contains("User ID,Username,Email,Full Name,Role,Active,Total Reservations,Total Spent,Registered At");
        // Check CSV escaping: commas and quotes
        assertThat(csv).contains("\"doe,john\"");
        assertThat(csv).contains("\"John \"\"The Driver\"\" Doe\"");
        assertThat(csv).contains("$150.00");
    }

    @Test
    @DisplayName("exportBookingTrends should generate valid CSV")
    void shouldExportBookingTrendsCsv() {
        BookingTrendsReportData.BookingTrendItem item = new BookingTrendsReportData.BookingTrendItem(
                55L, "driver@example.com", "Sam Wilson", "Metro Lot", "M-10", "METRO-1", Instant.now(), Instant.now().plusSeconds(3600), BigDecimal.valueOf(20.00), "CONFIRMED"
        );
        BookingTrendsReportData data = new BookingTrendsReportData(
                Instant.now().minusSeconds(86400), Instant.now(), 1L, Map.of("CONFIRMED", 1L), Map.of("Metro Lot", 1L), Map.of("2026-09-29", 1L), BigDecimal.valueOf(20.00), 1.0, List.of(item)
        );

        byte[] bytes = exporter.exportBookingTrends(data);
        String csv = new String(bytes, StandardCharsets.UTF_8);

        assertThat(csv).contains("# BOOKING TRENDS REPORT");
        assertThat(csv).contains("Reservation ID,Customer Email,Customer Name,Parking Lot,Spot Number,License Plate,Start Time,End Time,Total Amount,Status");
        assertThat(csv).contains("55,driver@example.com,Sam Wilson,Metro Lot,M-10,METRO-1");
    }

    @Test
    @DisplayName("exportPaymentSummary should generate valid CSV")
    void shouldExportPaymentSummaryCsv() {
        PaymentSummaryReportData.PaymentSummaryItem item = new PaymentSummaryReportData.PaymentSummaryItem(
                701L, "TX-998811", 55L, "payer@example.com", BigDecimal.valueOf(88.50), "USD", "CREDIT_CARD", "SUCCESS", Instant.now()
        );
        PaymentSummaryReportData data = new PaymentSummaryReportData(
                Instant.now().minusSeconds(86400), Instant.now(), 1L, BigDecimal.valueOf(88.50), Map.of("SUCCESS", 1L), Map.of("CREDIT_CARD", BigDecimal.valueOf(88.50)), Map.of("2026-09", BigDecimal.valueOf(88.50)), List.of(item)
        );

        byte[] bytes = exporter.exportPaymentSummary(data);
        String csv = new String(bytes, StandardCharsets.UTF_8);

        assertThat(csv).contains("# PAYMENT SUMMARY REPORT");
        assertThat(csv).contains("Payment ID,Transaction ID,Reservation ID,Customer Email,Amount,Currency,Payment Method,Status,Paid At");
        assertThat(csv).contains("701,TX-998811,55,payer@example.com,88.50,USD,CREDIT_CARD,SUCCESS");
    }
}
