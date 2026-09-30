package tech.lokum.parkinglot.report.service;

import tech.lokum.parkinglot.report.dto.ReportRequest;
import tech.lokum.parkinglot.report.dto.ReportResponse;
import tech.lokum.parkinglot.report.entity.ReportMetadata;
import tech.lokum.parkinglot.report.model.BookingTrendsReportData;
import tech.lokum.parkinglot.report.model.GeneratedReport;
import tech.lokum.parkinglot.report.model.PaymentSummaryReportData;
import tech.lokum.parkinglot.report.model.ReportFormat;
import tech.lokum.parkinglot.report.model.ReportType;
import tech.lokum.parkinglot.report.model.UserActivityReportData;

import java.time.Instant;

/**
 * Service for fetching, aggregating, and generating system reports in multiple formats,
 * as well as managing persisted report metadata.
 */
public interface ReportService {

    ReportResponse requestReport(ReportRequest request, String requestedBy);

    ReportMetadata getReportMetadata(Long reportId);

    GeneratedReport generateReport(ReportType type, ReportFormat format, Instant startDate, Instant endDate);

    GeneratedReport generateUserActivityReport(Instant startDate, Instant endDate, ReportFormat format);

    GeneratedReport generateBookingTrendsReport(Instant startDate, Instant endDate, ReportFormat format);

    GeneratedReport generatePaymentSummaryReport(Instant startDate, Instant endDate, ReportFormat format);

    UserActivityReportData fetchUserActivityData(Instant startDate, Instant endDate);

    BookingTrendsReportData fetchBookingTrendsData(Instant startDate, Instant endDate);

    PaymentSummaryReportData fetchPaymentSummaryData(Instant startDate, Instant endDate);
}
