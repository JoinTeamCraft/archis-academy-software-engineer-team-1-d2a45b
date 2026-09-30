package tech.lokum.parkinglot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import tech.lokum.parkinglot.entity.Feedback;

import java.time.Instant;

/**
 * DTO representing submitted feedback returned by the API.
 */
@Schema(description = "Feedback response object")
public record FeedbackResponse(
    @Schema(description = "Unique identifier of the feedback", example = "101")
    Long id,

    @Schema(description = "ID of the user who submitted the feedback", example = "1")
    Long userId,

    @Schema(description = "The feedback itself", example = "Great service!")
    String feedbackText,

    @Schema(description = "Rating from 1 (poor) to 5 (excellent)", example = "5")
    Integer rating,

    @Schema(description = "UTC timestamp when the feedback was submitted", example = "2025-01-23T10:00:00Z")
    Instant submittedAt
) {
    public static FeedbackResponse fromEntity(Feedback entity) {
        return new FeedbackResponse(
            entity.getId(),
            entity.getUser().getId(),
            entity.getFeedbackText(),
            entity.getRating(),
            entity.getCreatedAt()
        );
    }
}
