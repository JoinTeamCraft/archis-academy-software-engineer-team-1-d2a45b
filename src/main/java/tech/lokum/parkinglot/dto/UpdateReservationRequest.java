package tech.lokum.parkinglot.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import tech.lokum.parkinglot.entity.ReservationStatus;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

/**
 * DTO payload for modifying an existing parking reservation.
 */
@Schema(description = "Request body for updating an existing reservation")
public record UpdateReservationRequest(
    @Schema(description = "New reservation start timestamp in UTC (ISO-8601)", example = "2026-10-01T11:00:00Z")
    Instant startTime,

    @Schema(description = "New reservation end timestamp in UTC (ISO-8601)", example = "2026-10-01T13:00:00Z")
    Instant endTime,

    @Schema(description = "ID of new parking spot if changing spot", example = "2")
    Long parkingSpotId,

    @Schema(description = "ID of new vehicle if changing vehicle", example = "3")
    Long vehicleId,

    @Schema(description = "Updated reservation lifecycle status", example = "CANCELLED")
    ReservationStatus status
) {
    @JsonCreator
    public static UpdateReservationRequest create(
        @JsonProperty("startTime") String startTime,
        @JsonProperty("endTime") String endTime,
        @JsonProperty("parkingSpotId") Long parkingSpotId,
        @JsonProperty("vehicleId") Long vehicleId,
        @JsonProperty("status") ReservationStatus status
    ) {
        return new UpdateReservationRequest(
            parseFlexibleInstant(startTime),
            parseFlexibleInstant(endTime),
            parkingSpotId,
            vehicleId,
            status
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
