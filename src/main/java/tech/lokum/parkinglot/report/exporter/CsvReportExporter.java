package tech.lokum.parkinglot.report.exporter;

import org.springframework.stereotype.Component;
import tech.lokum.parkinglot.report.model.BookingTrendsReportData;
import tech.lokum.parkinglot.report.model.PaymentSummaryReportData;
import tech.lokum.parkinglot.report.model.ReportFormat;
import tech.lokum.parkinglot.report.model.UserActivityReportData;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Exporter generating RFC 4180 compliant CSV reports.
 */
@Component
public class CsvReportExporter implements ReportExporter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC);

    @Override
    public ReportFormat getFormat() {
        return ReportFormat.CSV;
    }

    @Override
    public byte[] exportUserActivity(UserActivityReportData data) {
        StringBuilder sb = new StringBuilder();

        // 1. Report Header & Meta
        sb.append("# USER ACTIVITY REPORT\n");
        sb.append("# Generated At: ").append(formatDate(Instant.now())).append("\n");
        sb.append("# Period: ").append(formatDate(data.getPeriodStart())).append(" to ").append(formatDate(data.getPeriodEnd())).append("\n");
        sb.append("# Total Users: ").append(data.getTotalUsers()).append("\n");
        sb.append("# Active Users: ").append(data.getActiveUsers()).append("\n");
        sb.append("# Inactive Users: ").append(data.getInactiveUsers()).append("\n");

        sb.append("# Users By Role: ");
        if (!data.getUsersByRole().isEmpty()) {
            boolean first = true;
            for (Map.Entry<String, Long> entry : data.getUsersByRole().entrySet()) {
                if (!first) sb.append("; ");
                sb.append(entry.getKey()).append("=").append(entry.getValue());
                first = false;
            }
        }
        sb.append("\n\n");

        // 2. Data Table
        sb.append("User ID,Username,Email,Full Name,Role,Active,Total Reservations,Total Spent,Registered At\n");
        for (UserActivityReportData.UserActivityItem item : data.getItems()) {
            sb.append(escape(item.getUserId())).append(",")
                    .append(escape(item.getUsername())).append(",")
                    .append(escape(item.getEmail())).append(",")
                    .append(escape(item.getFullName())).append(",")
                    .append(escape(item.getRole())).append(",")
                    .append(escape(item.isActive() ? "ACTIVE" : "INACTIVE")).append(",")
                    .append(escape(item.getTotalReservations())).append(",")
                    .append(escape(String.format("$%.2f", item.getTotalSpent()))).append(",")
                    .append(escape(formatDate(item.getRegisteredAt()))).append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportBookingTrends(BookingTrendsReportData data) {
        StringBuilder sb = new StringBuilder();

        // 1. Report Header & Meta
        sb.append("# BOOKING TRENDS REPORT\n");
        sb.append("# Generated At: ").append(formatDate(Instant.now())).append("\n");
        sb.append("# Period: ").append(formatDate(data.getPeriodStart())).append(" to ").append(formatDate(data.getPeriodEnd())).append("\n");
        sb.append("# Total Bookings: ").append(data.getTotalBookings()).append("\n");
        sb.append("# Total Revenue Booked: ").append(String.format("$%.2f", data.getTotalRevenue())).append("\n");
        sb.append("# Average Duration (Hours): ").append(String.format("%.2f", data.getAverageDurationHours())).append("\n");

        sb.append("# Bookings By Status: ");
        if (!data.getBookingsByStatus().isEmpty()) {
            boolean first = true;
            for (Map.Entry<String, Long> entry : data.getBookingsByStatus().entrySet()) {
                if (!first) sb.append("; ");
                sb.append(entry.getKey()).append("=").append(entry.getValue());
                first = false;
            }
        }
        sb.append("\n");

        sb.append("# Bookings By Parking Lot: ");
        if (!data.getBookingsByLot().isEmpty()) {
            boolean first = true;
            for (Map.Entry<String, Long> entry : data.getBookingsByLot().entrySet()) {
                if (!first) sb.append("; ");
                sb.append(entry.getKey()).append("=").append(entry.getValue());
                first = false;
            }
        }
        sb.append("\n\n");

        // 2. Data Table
        sb.append("Reservation ID,Customer Email,Customer Name,Parking Lot,Spot Number,License Plate,Start Time,End Time,Total Amount,Status\n");
        for (BookingTrendsReportData.BookingTrendItem item : data.getItems()) {
            sb.append(escape(item.getReservationId())).append(",")
                    .append(escape(item.getUserEmail())).append(",")
                    .append(escape(item.getCustomerName())).append(",")
                    .append(escape(item.getLotName())).append(",")
                    .append(escape(item.getSpotNumber())).append(",")
                    .append(escape(item.getVehiclePlate())).append(",")
                    .append(escape(formatDate(item.getStartTime()))).append(",")
                    .append(escape(formatDate(item.getEndTime()))).append(",")
                    .append(escape(String.format("$%.2f", item.getTotalAmount()))).append(",")
                    .append(escape(item.getStatus())).append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] exportPaymentSummary(PaymentSummaryReportData data) {
        StringBuilder sb = new StringBuilder();

        // 1. Report Header & Meta
        sb.append("# PAYMENT SUMMARY REPORT\n");
        sb.append("# Generated At: ").append(formatDate(Instant.now())).append("\n");
        sb.append("# Period: ").append(formatDate(data.getPeriodStart())).append(" to ").append(formatDate(data.getPeriodEnd())).append("\n");
        sb.append("# Total Transactions: ").append(data.getTotalTransactions()).append("\n");
        sb.append("# Total Revenue: ").append(String.format("$%.2f", data.getTotalRevenue())).append("\n");

        sb.append("# Transactions By Status: ");
        if (!data.getPaymentsByStatus().isEmpty()) {
            boolean first = true;
            for (Map.Entry<String, Long> entry : data.getPaymentsByStatus().entrySet()) {
                if (!first) sb.append("; ");
                sb.append(entry.getKey()).append("=").append(entry.getValue());
                first = false;
            }
        }
        sb.append("\n");

        sb.append("# Revenue By Payment Method: ");
        if (!data.getRevenueByMethod().isEmpty()) {
            boolean first = true;
            for (Map.Entry<String, java.math.BigDecimal> entry : data.getRevenueByMethod().entrySet()) {
                if (!first) sb.append("; ");
                sb.append(entry.getKey()).append("=").append(String.format("$%.2f", entry.getValue()));
                first = false;
            }
        }
        sb.append("\n\n");

        // 2. Data Table
        sb.append("Payment ID,Transaction ID,Reservation ID,Customer Email,Amount,Currency,Payment Method,Status,Paid At\n");
        for (PaymentSummaryReportData.PaymentSummaryItem item : data.getItems()) {
            sb.append(escape(item.getPaymentId())).append(",")
                    .append(escape(item.getTransactionId())).append(",")
                    .append(escape(item.getReservationId())).append(",")
                    .append(escape(item.getCustomerEmail())).append(",")
                    .append(escape(String.format("%.2f", item.getAmount()))).append(",")
                    .append(escape(item.getCurrency())).append(",")
                    .append(escape(item.getPaymentMethod())).append(",")
                    .append(escape(item.getStatus())).append(",")
                    .append(escape(formatDate(item.getPaidAt()))).append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String formatDate(Instant instant) {
        if (instant == null) return "N/A";
        return DATE_FORMATTER.format(instant);
    }

    private String escape(Object obj) {
        if (obj == null) return "";
        String s = String.valueOf(obj);
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
