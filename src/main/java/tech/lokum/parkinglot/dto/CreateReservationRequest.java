package tech.lokum.parkinglot.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import tech.lokum.parkinglot.entity.ReservationStatus;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

/**
 * DTO payload for creating a new parking spot reservation.
 */
@Schema(description = "Request body for reserving a parking spot")
public record CreateReservationRequest(
    @Schema(description = "Optional ID of the customer making the reservation (inferred from vehicle if omitted)", example = "1")
    Long userId,

    @NotNull(message = "Vehicle ID is required")
    @Schema(description = "ID of the vehicle being parked", example = "201")
    Long vehicleId,

    @NotNull(message = "Parking spot ID is required")
    @Schema(description = "ID of the parking spot to book", example = "101")
    Long parkingSpotId,

    @NotNull(message = "Start time is required")
    @Schema(description = "Reservation start timestamp in UTC (ISO-8601)", example = "2026-10-01T08:00:00")
    Instant startTime,

    @NotNull(message = "End time is required")
    @Schema(description = "Reservation end timestamp in UTC (ISO-8601)", example = "2026-10-01T10:00:00")
    Instant endTime
) {
    @JsonCreator
    public static CreateReservationRequest create(
        @JsonProperty("userId") Long userId,
        @JsonProperty("vehicleId") Long vehicleId,
        @JsonProperty("parkingSpotId") Long parkingSpotId,
        @JsonProperty("startTime") String startTime,
        @JsonProperty("endTime") String endTime
    ) {
        return new CreateReservationRequest(
            userId,
            vehicleId,
            parkingSpotId,
            parseFlexibleInstant(startTime),
            parseFlexibleInstant(endTime)
        );
    }

    private static Instant parseFlexibleInstant(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        text = text.trim();
        try {
            return Instant.parse(text);
        } catch (DateTimeParseException e) {
            try {
                return LocalDateTime.parse(text).toInstant(ZoneOffset.UTC);
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException(String.format("Invalid timestamp format '%s'. Expected ISO-8601 string.", text));
            }
        }
    }
}
