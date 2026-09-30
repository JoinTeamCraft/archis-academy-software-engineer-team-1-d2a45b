package tech.lokum.parkinglot.export.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;

/**
 * DTO capturing request criteria for exporting system data.
 */
@Schema(description = "Request criteria for exporting system datasets")
public class DataExportRequest {

    @NotBlank(message = "Data type is required (e.g. 'bookings', 'users', 'payments')")
    @Schema(description = "Type of dataset to export: 'bookings', 'users', 'payments', 'parking_lots'", example = "bookings", requiredMode = Schema.RequiredMode.REQUIRED)
    private String dataType;

    @Schema(description = "Export file format: 'CSV' or 'JSON'", example = "CSV", defaultValue = "CSV")
    private String format = "CSV";

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(description = "Optional filter start timestamp (ISO-8601)", example = "2026-01-01T00:00:00Z")
    private Instant startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(description = "Optional filter end timestamp (ISO-8601)", example = "2026-12-31T23:59:59Z")
    private Instant endDate;

    @Schema(description = "Optional status filter (e.g., CONFIRMED, COMPLETED, SUCCESS)", example = "CONFIRMED")
    private String status;

    public DataExportRequest() {
    }

    public DataExportRequest(String dataType, String format) {
        this.dataType = dataType;
        this.format = format;
    }

    public DataExportRequest(String dataType, String format, Instant startDate, Instant endDate, String status) {
        this.dataType = dataType;
        this.format = format;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public void setEndDate(Instant endDate) {
        this.endDate = endDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
