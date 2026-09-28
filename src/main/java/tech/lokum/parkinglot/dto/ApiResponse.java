package tech.lokum.parkinglot.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Generic response wrapper for single item or action responses.
 *
 * @param <T> payload type
 */
@Schema(description = "Generic API response wrapper")
public record ApiResponse<T>(
    @Schema(description = "Indicates whether the request was successful", example = "true")
    boolean success,

    @Schema(description = "Human-readable summary message", example = "Operation completed successfully")
    String message,

    @Schema(description = "Response data payload")
    T data
) {
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "Operation completed successfully", data);
    }
}
