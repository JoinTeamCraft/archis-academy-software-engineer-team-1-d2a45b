package tech.lokum.parkinglot.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Standard payload returned on validation failures containing error description and detail messages.
 */
@Schema(description = "Validation failure response containing error and list of details")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ValidationErrorResponse(
    @Schema(description = "Error description", example = "Validation failed")
    String error,

    @Schema(description = "List of validation failure details", example = "[\"Username must be unique\", \"Password must contain at least 8 characters\"]")
    List<String> details
) {
}
