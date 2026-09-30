package tech.lokum.parkinglot.export.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import tech.lokum.parkinglot.dto.ErrorResponse;
import tech.lokum.parkinglot.exception.BadRequestException;
import tech.lokum.parkinglot.export.dto.DataExportRequest;
import tech.lokum.parkinglot.export.dto.ExportedFile;
import tech.lokum.parkinglot.export.model.ExportDataType;
import tech.lokum.parkinglot.export.model.ExportFormat;
import tech.lokum.parkinglot.export.service.DataExportService;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * REST controller for exporting system datasets (bookings, users, payments, parking lots)
 * in CSV or JSON format. Accessible only to authorized roles (ADMIN, MANAGER, OPERATOR).
 */
@RestController
@RequestMapping("/api/data")
@Tag(name = "Data Export", description = "Endpoints for exporting system datasets into CSV and JSON files")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'OPERATOR')")
public class DataExportController {

    private static final DateTimeFormatter FILE_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").withZone(ZoneOffset.UTC);

    private final DataExportService dataExportService;

    public DataExportController(DataExportService dataExportService) {
        this.dataExportService = dataExportService;
    }

    @GetMapping("/export")
    @Operation(
            summary = "Export system data",
            description = "Exports system datasets (bookings, users, payments, parking_lots) into a downloadable CSV or JSON file. Supports parameters via JSON body or query parameters."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Dataset exported successfully as downloadable file attachment",
                    content = @Content(mediaType = "text/csv")
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid data type or export format requested",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Authentication required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Requires ADMIN, MANAGER, or OPERATOR role",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    public ResponseEntity<byte[]> exportData(
            @RequestBody(required = false) DataExportRequest requestBody,
            @Parameter(description = "Data type to export: 'bookings', 'users', 'payments', 'parking_lots'")
            @RequestParam(required = false) String dataType,
            @Parameter(description = "Format: 'CSV' or 'JSON'")
            @RequestParam(required = false) String format,
            @Parameter(description = "Optional filter start timestamp (ISO-8601)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @Parameter(description = "Optional filter end timestamp (ISO-8601)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @Parameter(description = "Optional status filter")
            @RequestParam(required = false) String status
    ) {
        DataExportRequest resolvedRequest = resolveRequest(requestBody, dataType, format, startDate, endDate, status);
        ExportedFile exportedFile = dataExportService.exportData(resolvedRequest);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(exportedFile.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + exportedFile.getFileName() + "\"")
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION + ", X-Export-Records")
                .header("X-Export-Records", String.valueOf(exportedFile.getRecordCount()))
                .body(exportedFile.getContent());
    }

    @PostMapping("/export")
    @Operation(
            summary = "Export system data (POST)",
            description = "Alternative POST endpoint for exporting datasets with JSON request payload."
    )
    public ResponseEntity<byte[]> exportDataPost(@RequestBody DataExportRequest request) {
        return exportData(request, null, null, null, null, null);
    }

    @GetMapping("/export/stream")
    @Operation(
            summary = "Stream system data export",
            description = "Streams dataset export using chunked transfer encoding directly to the client."
    )
    public ResponseEntity<StreamingResponseBody> streamExportData(
            @RequestBody(required = false) DataExportRequest requestBody,
            @RequestParam(required = false) String dataType,
            @RequestParam(required = false) String format,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(required = false) String status
    ) {
        DataExportRequest resolvedRequest = resolveRequest(requestBody, dataType, format, startDate, endDate, status);
        ExportDataType exportDataType = ExportDataType.from(resolvedRequest.getDataType());
        ExportFormat exportFormat = ExportFormat.from(resolvedRequest.getFormat());

        String timestamp = FILE_TIMESTAMP_FORMATTER.format(Instant.now());
        String fileName = exportDataType.name().toLowerCase() + "_export_" + timestamp + exportFormat.getExtension();

        StreamingResponseBody responseBody = outputStream -> dataExportService.streamExportData(resolvedRequest, outputStream);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(exportFormat.getMediaType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                .body(responseBody);
    }

    private DataExportRequest resolveRequest(
            DataExportRequest requestBody,
            String dataType,
            String format,
            Instant startDate,
            Instant endDate,
            String status
    ) {
        if (requestBody != null && requestBody.getDataType() != null && !requestBody.getDataType().isBlank()) {
            if (format != null && !format.isBlank() && (requestBody.getFormat() == null || requestBody.getFormat().isBlank())) {
                requestBody.setFormat(format);
            }
            if (startDate != null && requestBody.getStartDate() == null) {
                requestBody.setStartDate(startDate);
            }
            if (endDate != null && requestBody.getEndDate() == null) {
                requestBody.setEndDate(endDate);
            }
            if (status != null && !status.isBlank() && (requestBody.getStatus() == null || requestBody.getStatus().isBlank())) {
                requestBody.setStatus(status);
            }
            return requestBody;
        }

        if (dataType == null || dataType.isBlank()) {
            throw new BadRequestException("Data type is required (e.g. 'bookings', 'users', 'payments')");
        }

        return new DataExportRequest(dataType, format != null ? format : "CSV", startDate, endDate, status);
    }
}
