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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tech.lokum.parkinglot.report.dto.ReportRequest;
import tech.lokum.parkinglot.report.dto.ReportResponse;
import tech.lokum.parkinglot.report.entity.ReportMetadata;
import tech.lokum.parkinglot.report.model.BookingTrendsReportData;
import tech.lokum.parkinglot.report.model.GeneratedReport;
import tech.lokum.parkinglot.report.model.PaymentSummaryReportData;
import tech.lokum.parkinglot.report.model.ReportFormat;
import tech.lokum.parkinglot.report.model.UserActivityReportData;
import tech.lokum.parkinglot.report.service.ReportService;

import java.time.Instant;

/**
 * REST controller for requesting, generating, and downloading analytical reports
 * such as user activity, booking trends, and payment summaries.
 */
@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
@Tag(name = "Reports", description = "Endpoints for generating and downloading system reports (User Activity, Booking Trends, Payment Summaries)")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @Operation(
            summary = "Request a system report",
            description = "Requests generation of a report (e.g., monthlyRevenue, userActivity, bookingTrends, paymentSummary) and returns download link metadata."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Report generated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid report parameters"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN, MANAGER, or OPERATOR role")
    })
    @GetMapping
    public ResponseEntity<ReportResponse> requestReport(
            @RequestBody(required = false) ReportRequest requestBody,
            @Parameter(description = "Report type (e.g., 'monthlyRevenue', 'userActivity', 'bookingTrends')") @RequestParam(required = false) String reportType,
            @Parameter(description = "Filter start date (e.g., '2025-01-01')") @RequestParam(required = false) String startDate,
            @Parameter(description = "Filter end date (e.g., '2025-01-31')") @RequestParam(required = false) String endDate,
            @Parameter(description = "Export format (CSV or PDF)") @RequestParam(required = false, defaultValue = "CSV") String format,
            Authentication authentication
    ) {
        ReportRequest request;
        if (requestBody != null && requestBody.getReportType() != null && !requestBody.getReportType().isBlank()) {
            request = requestBody;
            if (request.getFormat() == null || request.getFormat().isBlank()) {
                request.setFormat(format);
            }
        } else {
            request = new ReportRequest(reportType, startDate, endDate, format);
        }

        String requestedBy = authentication != null ? authentication.getName() : "system";
        ReportResponse response = reportService.requestReport(request, requestedBy);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Request report via POST", description = "Alternative POST endpoint for requesting report generation.")
    @PostMapping
    public ResponseEntity<ReportResponse> requestReportPost(
            @RequestBody ReportRequest request,
            Authentication authentication
    ) {
        String requestedBy = authentication != null ? authentication.getName() : "system";
        ReportResponse response = reportService.requestReport(request, requestedBy);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Download generated report file by ID", description = "Streams the stored binary file for a previously generated report.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Report file streamed successfully"),
            @ApiResponse(responseCode = "404", description = "Report not found")
    })
    @GetMapping("/download/{reportId}")
    public ResponseEntity<byte[]> downloadReport(@PathVariable Long reportId) {
        ReportMetadata metadata = reportService.getReportMetadata(reportId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(metadata.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + metadata.getFileName() + "\"")
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                .body(metadata.getContent());
    }

    @Operation(summary = "Direct Download User Activity Report", description = "Exports user registration, status, and activity metrics as CSV or PDF.")
    @GetMapping("/user-activity")
    public ResponseEntity<byte[]> downloadUserActivityReport(
            @Parameter(description = "Export format (CSV or PDF)") @RequestParam(defaultValue = "CSV") ReportFormat format,
            @Parameter(description = "Filter start timestamp") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @Parameter(description = "Filter end timestamp") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate
    ) {
        GeneratedReport report = reportService.generateUserActivityReport(startDate, endDate, format);
        return toResponseEntity(report);
    }

    @Operation(summary = "Direct Download Booking Trends Report", description = "Exports reservation volumes, parking lot utilization, and duration trends as CSV or PDF.")
    @GetMapping("/booking-trends")
    public ResponseEntity<byte[]> downloadBookingTrendsReport(
            @Parameter(description = "Export format (CSV or PDF)") @RequestParam(defaultValue = "CSV") ReportFormat format,
            @Parameter(description = "Filter start timestamp") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @Parameter(description = "Filter end timestamp") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate
    ) {
        GeneratedReport report = reportService.generateBookingTrendsReport(startDate, endDate, format);
        return toResponseEntity(report);
    }

    @Operation(summary = "Direct Download Payment Summary Report", description = "Exports revenue breakdown, settlement methods, and transaction logs as CSV or PDF.")
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
