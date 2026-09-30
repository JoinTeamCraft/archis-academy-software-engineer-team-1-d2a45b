package tech.lokum.parkinglot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO payload for submitting user feedback.
 */
@Schema(description = "Request body for submitting feedback about the application or service")
public record CreateFeedbackRequest(
    @NotNull(message = "User ID is required")
    @Schema(description = "ID of the user submitting the feedback", example = "1")
    Long userId,

    @NotBlank(message = "Feedback text is required")
    @Size(max = 1000, message = "Feedback text must be at most 1000 characters")
    @Schema(description = "The feedback itself", example = "Great service!")
    String feedbackText,

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    @Schema(description = "Rating from 1 (poor) to 5 (excellent)", example = "5", minimum = "1", maximum = "5")
    Integer rating
) {
}
