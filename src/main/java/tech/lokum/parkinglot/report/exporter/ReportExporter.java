package tech.lokum.parkinglot.report.exporter;

import tech.lokum.parkinglot.report.model.BookingTrendsReportData;
import tech.lokum.parkinglot.report.model.PaymentSummaryReportData;
import tech.lokum.parkinglot.report.model.ReportFormat;
import tech.lokum.parkinglot.report.model.UserActivityReportData;

/**
 * Strategy interface for exporting report datasets to target binary/text formats.
 */
public interface ReportExporter {

    ReportFormat getFormat();

    byte[] exportUserActivity(UserActivityReportData data);

    byte[] exportBookingTrends(BookingTrendsReportData data);

    byte[] exportPaymentSummary(PaymentSummaryReportData data);
}
