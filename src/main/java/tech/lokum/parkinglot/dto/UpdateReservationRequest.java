package tech.lokum.parkinglot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import tech.lokum.parkinglot.entity.ReservationStatus;

import java.time.Instant;

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
}
