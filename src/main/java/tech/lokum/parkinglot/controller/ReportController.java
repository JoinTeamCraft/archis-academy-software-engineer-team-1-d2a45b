package tech.lokum.parkinglot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tech.lokum.parkinglot.report.model.BookingTrendsReportData;
import tech.lokum.parkinglot.report.model.GeneratedReport;
import tech.lokum.parkinglot.report.model.PaymentSummaryReportData;
import tech.lokum.parkinglot.report.model.ReportFormat;
import tech.lokum.parkinglot.report.model.ReportType;
import tech.lokum.parkinglot.report.model.UserActivityReportData;
import tech.lokum.parkinglot.report.service.ReportService;

import java.time.Instant;

/**
 * REST controller for generating and downloading analytical reports
 * such as user activity, booking trends, and payment summaries.
 */
@RestController
@RequestMapping("/api/reports")
@Tag(name = "Reports", description = "Endpoints for generating system reports (User Activity, Booking Trends, Payment Summaries)")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @Operation(summary = "Generate system report file", description = "Generates and downloads a report (User Activity, Booking Trends, or Payment Summary) in CSV or PDF format.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Report generated and streamed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid report parameters")
    })
    @GetMapping
    public ResponseEntity<byte[]> generateReport(
            @Parameter(description = "Type of report to generate") @RequestParam ReportType type,
            @Parameter(description = "Export format (CSV or PDF)") @RequestParam(defaultValue = "CSV") ReportFormat format,
            @Parameter(description = "Optional filter start timestamp (ISO-8601)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @Parameter(description = "Optional filter end timestamp (ISO-8601)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate
    ) {
        GeneratedReport report = reportService.generateReport(type, format, startDate, endDate);
        return toResponseEntity(report);
    }

    @Operation(summary = "Download User Activity Report", description = "Exports user registration, status, and activity metrics as CSV or PDF.")
    @GetMapping("/user-activity")
    public ResponseEntity<byte[]> downloadUserActivityReport(
            @Parameter(description = "Export format (CSV or PDF)") @RequestParam(defaultValue = "CSV") ReportFormat format,
            @Parameter(description = "Filter start timestamp") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @Parameter(description = "Filter end timestamp") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate
    ) {
        GeneratedReport report = reportService.generateUserActivityReport(startDate, endDate, format);
        return toResponseEntity(report);
    }

    @Operation(summary = "Download Booking Trends Report", description = "Exports reservation volumes, parking lot utilization, and duration trends as CSV or PDF.")
    @GetMapping("/booking-trends")
    public ResponseEntity<byte[]> downloadBookingTrendsReport(
            @Parameter(description = "Export format (CSV or PDF)") @RequestParam(defaultValue = "CSV") ReportFormat format,
            @Parameter(description = "Filter start timestamp") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @Parameter(description = "Filter end timestamp") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate
    ) {
        GeneratedReport report = reportService.generateBookingTrendsReport(startDate, endDate, format);
        return toResponseEntity(report);
    }

    @Operation(summary = "Download Payment Summary Report", description = "Exports revenue breakdown, settlement methods, and transaction logs as CSV or PDF.")
    @GetMapping("/payment-summary")
    public ResponseEntity<byte[]> downloadPaymentSummaryReport(
            @Parameter(description = "Export format (CSV or PDF)") @RequestParam(defaultValue = "CSV") ReportFormat format,
            @Parameter(description = "Filter start timestamp") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @Parameter(description = "Filter end timestamp") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate
    ) {
        GeneratedReport report = reportService.generatePaymentSummaryReport(startDate, endDate, format);
        return toResponseEntity(report);
    }

    @Operation(summary = "Fetch User Activity data in JSON", description = "Retrieves raw aggregated metrics for user activity.")
    @GetMapping("/data/user-activity")
    public ResponseEntity<UserActivityReportData> getUserActivityData(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate
    ) {
        return ResponseEntity.ok(reportService.fetchUserActivityData(startDate, endDate));
    }

    @Operation(summary = "Fetch Booking Trends data in JSON", description = "Retrieves raw aggregated metrics for booking trends.")
    @GetMapping("/data/booking-trends")
    public ResponseEntity<BookingTrendsReportData> getBookingTrendsData(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate
    ) {
        return ResponseEntity.ok(reportService.fetchBookingTrendsData(startDate, endDate));
    }

    @Operation(summary = "Fetch Payment Summary data in JSON", description = "Retrieves raw aggregated metrics for payment summaries.")
    @GetMapping("/data/payment-summary")
    public ResponseEntity<PaymentSummaryReportData> getPaymentSummaryData(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate
    ) {
        return ResponseEntity.ok(reportService.fetchPaymentSummaryData(startDate, endDate));
    }

    private ResponseEntity<byte[]> toResponseEntity(GeneratedReport report) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(report.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + report.getFileName() + "\"")
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                .body(report.getContent());
    }
}
