package tech.lokum.parkinglot.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Standardized error response returned by the API for all error conditions.
 */
@Schema(description = "Standardized error response body")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    @Schema(description = "UTC timestamp when the error occurred", example = "2026-09-26T16:30:00Z")
    Instant timestamp,

    @Schema(description = "HTTP status code", example = "400")
    Integer status,

    @Schema(description = "HTTP status reason or error category", example = "Bad Request")
    String error,

    @Schema(description = "Descriptive error message", example = "Validation failed for request")
    String message,

    @Schema(description = "Request path that triggered the error", example = "/api/v1/reservations")
    String path,

    @Schema(description = "Field-level validation error details when applicable")
    Map<String, String> validationErrors,

    @Schema(description = "Detailed list of validation or error messages")
    List<String> details
) {
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(Instant.now(), status, error, message, path, null, null);
    }

    public static ErrorResponse of(int status, String error, String message, String path, Map<String, String> validationErrors) {
        List<String> details = validationErrors != null ? List.copyOf(validationErrors.values()) : null;
        return new ErrorResponse(Instant.now(), status, error, message, path, validationErrors, details);
    }

    public static ErrorResponse of(int status, String error, String message, String path, Map<String, String> validationErrors, List<String> details) {
        return new ErrorResponse(Instant.now(), status, error, message, path, validationErrors, details);
    }

    public static ErrorResponse validation(String error, List<String> details) {
        return new ErrorResponse(null, null, error, null, null, null, details);
    }
}
