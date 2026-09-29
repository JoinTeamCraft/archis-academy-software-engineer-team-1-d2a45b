package tech.lokum.parkinglot.report.exporter;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;
import tech.lokum.parkinglot.report.model.BookingTrendsReportData;
import tech.lokum.parkinglot.report.model.PaymentSummaryReportData;
import tech.lokum.parkinglot.report.model.ReportFormat;
import tech.lokum.parkinglot.report.model.UserActivityReportData;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Exporter generating styled, professional PDF reports using OpenPDF.
 */
@Component
public class PdfReportExporter implements ReportExporter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withZone(ZoneOffset.UTC);

    private static final Color PRIMARY_COLOR = new Color(30, 64, 175);     // Slate Blue #1e40af
    private static final Color HEADER_BG = new Color(241, 245, 249);       // Light Slate #f1f5f9
    private static final Color ROW_ALT_BG = new Color(248, 250, 252);      // Very light slate #f8fafc
    private static final Color BORDER_COLOR = new Color(226, 232, 240);    // Border #e2e8f0

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.WHITE);
    private static final Font SUBTITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(219, 234, 254));
    private static final Font SECTION_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(15, 23, 42));
    private static final Font TH_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, new Color(30, 41, 59));
    private static final Font TD_FONT = FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(51, 65, 85));
    private static final Font META_FONT = FontFactory.getFont(FontFactory.HELVETICA, 9, new Color(71, 85, 105));

    @Override
    public ReportFormat getFormat() {
        return ReportFormat.PDF;
    }

    @Override
    public byte[] exportUserActivity(UserActivityReportData data) {
        return buildPdf(document -> {
            addHeaderBanner(document, "User Activity Report", "System-wide user registration, activity status, and engagement breakdown");

            // KPI Summary
            addSectionTitle(document, "Executive Summary");
            PdfPTable kpiTable = new PdfPTable(4);
            kpiTable.setWidthPercentage(100);
            kpiTable.setSpacingAfter(15);

            addKpiCell(kpiTable, "Total Users", String.valueOf(data.getTotalUsers()));
            addKpiCell(kpiTable, "Active Users", String.valueOf(data.getActiveUsers()));
            addKpiCell(kpiTable, "Inactive Users", String.valueOf(data.getInactiveUsers()));

            StringBuilder roleStr = new StringBuilder();
            data.getUsersByRole().forEach((role, count) -> {
                if (roleStr.length() > 0) roleStr.append(", ");
                roleStr.append(role).append(": ").append(count);
            });
            addKpiCell(kpiTable, "Roles Breakdown", roleStr.length() > 0 ? roleStr.toString() : "None");
            document.add(kpiTable);

            // Detail Table
            addSectionTitle(document, "User Directory & Activity Details");
            PdfPTable table = new PdfPTable(8);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.0f, 2.0f, 2.5f, 2.5f, 1.5f, 1.2f, 1.5f, 1.8f});

            addHeaderCell(table, "ID");
            addHeaderCell(table, "Username");
            addHeaderCell(table, "Email");
            addHeaderCell(table, "Full Name");
            addHeaderCell(table, "Role");
            addHeaderCell(table, "Status");
            addHeaderCell(table, "Bookings");
            addHeaderCell(table, "Total Spent");

            boolean alt = false;
            for (UserActivityReportData.UserActivityItem item : data.getItems()) {
                Color bg = alt ? ROW_ALT_BG : Color.WHITE;
                addDataCell(table, String.valueOf(item.getUserId()), Element.ALIGN_CENTER, bg);
                addDataCell(table, item.getUsername() != null ? item.getUsername() : "-", Element.ALIGN_LEFT, bg);
                addDataCell(table, item.getEmail(), Element.ALIGN_LEFT, bg);
                addDataCell(table, item.getFullName() != null ? item.getFullName() : "-", Element.ALIGN_LEFT, bg);
                addDataCell(table, item.getRole(), Element.ALIGN_CENTER, bg);
                addDataCell(table, item.isActive() ? "Active" : "Inactive", Element.ALIGN_CENTER, bg);
                addDataCell(table, String.valueOf(item.getTotalReservations()), Element.ALIGN_RIGHT, bg);
                addDataCell(table, String.format("$%.2f", item.getTotalSpent()), Element.ALIGN_RIGHT, bg);
                alt = !alt;
            }
            document.add(table);
        });
    }

    @Override
    public byte[] exportBookingTrends(BookingTrendsReportData data) {
        return buildPdf(document -> {
            addHeaderBanner(document, "Booking Trends Report", "Reservation volume, parking spot utilization, and booking revenue trends");

            // KPI Summary
            addSectionTitle(document, "Executive Summary");
            PdfPTable kpiTable = new PdfPTable(4);
            kpiTable.setWidthPercentage(100);
            kpiTable.setSpacingAfter(15);

            addKpiCell(kpiTable, "Total Bookings", String.valueOf(data.getTotalBookings()));
            addKpiCell(kpiTable, "Total Revenue Booked", String.format("$%.2f", data.getTotalRevenue()));
            addKpiCell(kpiTable, "Avg Duration (Hrs)", String.format("%.2f", data.getAverageDurationHours()));

            StringBuilder statusStr = new StringBuilder();
            data.getBookingsByStatus().forEach((st, cnt) -> {
                if (statusStr.length() > 0) statusStr.append(", ");
                statusStr.append(st).append(": ").append(cnt);
            });
            addKpiCell(kpiTable, "Status Breakdown", statusStr.length() > 0 ? statusStr.toString() : "None");
            document.add(kpiTable);

            // Detail Table
            addSectionTitle(document, "Reservation Details");
            PdfPTable table = new PdfPTable(9);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.0f, 2.2f, 2.0f, 2.0f, 1.2f, 1.5f, 2.2f, 2.2f, 1.5f});

            addHeaderCell(table, "ID");
            addHeaderCell(table, "Customer Email");
            addHeaderCell(table, "Customer Name");
            addHeaderCell(table, "Lot Name");
            addHeaderCell(table, "Spot");
            addHeaderCell(table, "Plate");
            addHeaderCell(table, "Start Time");
            addHeaderCell(table, "End Time");
            addHeaderCell(table, "Amount");

            boolean alt = false;
            for (BookingTrendsReportData.BookingTrendItem item : data.getItems()) {
                Color bg = alt ? ROW_ALT_BG : Color.WHITE;
                addDataCell(table, "#" + item.getReservationId(), Element.ALIGN_CENTER, bg);
                addDataCell(table, item.getUserEmail(), Element.ALIGN_LEFT, bg);
                addDataCell(table, item.getCustomerName(), Element.ALIGN_LEFT, bg);
                addDataCell(table, item.getLotName(), Element.ALIGN_LEFT, bg);
                addDataCell(table, item.getSpotNumber(), Element.ALIGN_CENTER, bg);
                addDataCell(table, item.getVehiclePlate(), Element.ALIGN_CENTER, bg);
                addDataCell(table, formatDate(item.getStartTime()), Element.ALIGN_LEFT, bg);
                addDataCell(table, formatDate(item.getEndTime()), Element.ALIGN_LEFT, bg);
                addDataCell(table, String.format("$%.2f", item.getTotalAmount()), Element.ALIGN_RIGHT, bg);
                alt = !alt;
            }
            document.add(table);
        });
    }

    @Override
    public byte[] exportPaymentSummary(PaymentSummaryReportData data) {
        return buildPdf(document -> {
            addHeaderBanner(document, "Payment Summary Report", "Payment transaction logs, settlement methods, and revenue reconciliations");

            // KPI Summary
            addSectionTitle(document, "Executive Summary");
            PdfPTable kpiTable = new PdfPTable(4);
            kpiTable.setWidthPercentage(100);
            kpiTable.setSpacingAfter(15);

            addKpiCell(kpiTable, "Total Transactions", String.valueOf(data.getTotalTransactions()));
            addKpiCell(kpiTable, "Total Revenue", String.format("$%.2f", data.getTotalRevenue()));

            StringBuilder methodStr = new StringBuilder();
            data.getRevenueByMethod().forEach((m, amt) -> {
                if (methodStr.length() > 0) methodStr.append(", ");
                methodStr.append(m).append(": $").append(amt);
            });
            addKpiCell(kpiTable, "Revenue By Method", methodStr.length() > 0 ? methodStr.toString() : "None");

            StringBuilder statusStr = new StringBuilder();
            data.getPaymentsByStatus().forEach((st, cnt) -> {
                if (statusStr.length() > 0) statusStr.append(", ");
                statusStr.append(st).append(": ").append(cnt);
            });
            addKpiCell(kpiTable, "Status Breakdown", statusStr.length() > 0 ? statusStr.toString() : "None");
            document.add(kpiTable);

            // Detail Table
            addSectionTitle(document, "Payment Transactions");
            PdfPTable table = new PdfPTable(8);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.0f, 2.2f, 1.2f, 2.5f, 1.5f, 1.8f, 1.5f, 2.2f});

            addHeaderCell(table, "ID");
            addHeaderCell(table, "Transaction ID");
            addHeaderCell(table, "Res #");
            addHeaderCell(table, "Customer Email");
            addHeaderCell(table, "Amount");
            addHeaderCell(table, "Method");
            addHeaderCell(table, "Status");
            addHeaderCell(table, "Paid At");

            boolean alt = false;
            for (PaymentSummaryReportData.PaymentSummaryItem item : data.getItems()) {
                Color bg = alt ? ROW_ALT_BG : Color.WHITE;
                addDataCell(table, "#" + item.getPaymentId(), Element.ALIGN_CENTER, bg);
                addDataCell(table, item.getTransactionId(), Element.ALIGN_LEFT, bg);
                addDataCell(table, item.getReservationId() != null ? "#" + item.getReservationId() : "-", Element.ALIGN_CENTER, bg);
                addDataCell(table, item.getCustomerEmail(), Element.ALIGN_LEFT, bg);
                addDataCell(table, String.format("$%.2f %s", item.getAmount(), item.getCurrency()), Element.ALIGN_RIGHT, bg);
                addDataCell(table, item.getPaymentMethod(), Element.ALIGN_CENTER, bg);
                addDataCell(table, item.getStatus(), Element.ALIGN_CENTER, bg);
                addDataCell(table, formatDate(item.getPaidAt()), Element.ALIGN_LEFT, bg);
                alt = !alt;
            }
            document.add(table);
        });
    }

    private byte[] buildPdf(PdfDocumentWriter writer) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 24, 24, 24, 24);
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            writer.write(document);
            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("Failed to generate PDF document", e);
        }
    }

    private void addHeaderBanner(Document document, String title, String subtitle) throws DocumentException {
        PdfPTable banner = new PdfPTable(1);
        banner.setWidthPercentage(100);
        banner.setSpacingAfter(15);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(PRIMARY_COLOR);
        cell.setPadding(14);
        cell.setBorder(0);

        Paragraph pTitle = new Paragraph(title, TITLE_FONT);
        Paragraph pSub = new Paragraph(subtitle + " | Generated: " + DATE_FORMATTER.format(Instant.now()), SUBTITLE_FONT);
        pSub.setSpacingBefore(4);

        cell.addElement(pTitle);
        cell.addElement(pSub);
        banner.addCell(cell);
        document.add(banner);
    }

    private void addSectionTitle(Document document, String title) throws DocumentException {
        Paragraph p = new Paragraph(title, SECTION_FONT);
        p.setSpacingBefore(8);
        p.setSpacingAfter(6);
        document.add(p);
    }

    private void addKpiCell(PdfPTable table, String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(HEADER_BG);
        cell.setPadding(8);
        cell.setBorderColor(BORDER_COLOR);

        Paragraph pLabel = new Paragraph(label.toUpperCase(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, new Color(100, 116, 139)));
        Paragraph pVal = new Paragraph(value, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, new Color(15, 23, 42)));
        pVal.setSpacingBefore(2);

        cell.addElement(pLabel);
        cell.addElement(pVal);
        table.addCell(cell);
    }

    private void addHeaderCell(PdfPTable table, String header) {
        PdfPCell cell = new PdfPCell(new Phrase(header, TH_FONT));
        cell.setBackgroundColor(HEADER_BG);
        cell.setPadding(6);
        cell.setBorderColor(BORDER_COLOR);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addDataCell(PdfPTable table, String text, int align, Color bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "-", TD_FONT));
        cell.setBackgroundColor(bgColor);
        cell.setPadding(5);
        cell.setBorderColor(BORDER_COLOR);
        cell.setHorizontalAlignment(align);
        table.addCell(cell);
    }

    private String formatDate(Instant instant) {
        if (instant == null) return "N/A";
        return DATE_FORMATTER.format(instant);
    }

    @FunctionalInterface
    private interface PdfDocumentWriter {
        void write(Document document) throws DocumentException;
    }
}
