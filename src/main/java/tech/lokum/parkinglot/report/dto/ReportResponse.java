package tech.lokum.parkinglot.report.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response payload returned upon successful generation and storage of a report.
 */
@Schema(description = "Response containing metadata and download link for a generated report")
public class ReportResponse {

    @Schema(description = "Unique identifier of the generated report metadata record", example = "123")
    @JsonProperty("reportId")
    private Long reportId;

    @Schema(description = "Lifecycle status of the report", example = "generated")
    @JsonProperty("status")
    private String status;

    @Schema(description = "Relative URL path to stream or download the report file", example = "/api/reports/download/123")
    @JsonProperty("downloadLink")
    private String downloadLink;

    public ReportResponse() {
    }

    public ReportResponse(Long reportId, String status, String downloadLink) {
        this.reportId = reportId;
        this.status = status;
        this.downloadLink = downloadLink;
    }

    public Long getReportId() {
        return reportId;
    }

    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDownloadLink() {
        return downloadLink;
    }

    public void setDownloadLink(String downloadLink) {
        this.downloadLink = downloadLink;
    }
}
