package tech.lokum.parkinglot.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for initiating a report generation request.
 */
@Schema(description = "Payload for requesting a system report")
public class ReportRequest {

    @NotBlank(message = "reportType is required")
    @Schema(description = "Report type (e.g., 'monthlyRevenue', 'userActivity', 'bookingTrends', 'paymentSummary')", example = "monthlyRevenue")
    private String reportType;

    @Schema(description = "Start date (ISO-8601 string, e.g., '2025-01-01')", example = "2025-01-01")
    private String startDate;

    @Schema(description = "End date (ISO-8601 string, e.g., '2025-01-31')", example = "2025-01-31")
    private String endDate;

    @Schema(description = "Export format ('CSV' or 'PDF')", example = "CSV", defaultValue = "CSV")
    private String format = "CSV";

    public ReportRequest() {
    }

    public ReportRequest(String reportType, String startDate, String endDate) {
        this(reportType, startDate, endDate, "CSV");
    }

    public ReportRequest(String reportType, String startDate, String endDate, String format) {
        this.reportType = reportType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.format = (format != null && !format.isBlank()) ? format : "CSV";
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }
}
