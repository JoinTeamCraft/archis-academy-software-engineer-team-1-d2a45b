package tech.lokum.parkinglot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * DTO payload for creating a new parking spot reservation.
 */
@Schema(description = "Request body for reserving a parking spot")
public record CreateReservationRequest(
    @NotNull(message = "User ID is required")
    @Schema(description = "ID of the customer making the reservation", example = "1")
    Long userId,

    @NotNull(message = "Vehicle ID is required")
    @Schema(description = "ID of the vehicle being parked", example = "1")
    Long vehicleId,

    @NotNull(message = "Parking spot ID is required")
    @Schema(description = "ID of the parking spot to book", example = "1")
    Long parkingSpotId,

    @NotNull(message = "Start time is required")
    @Schema(description = "Reservation start timestamp in UTC (ISO-8601)", example = "2026-10-01T10:00:00Z")
    Instant startTime,

    @NotNull(message = "End time is required")
    @Schema(description = "Reservation end timestamp in UTC (ISO-8601)", example = "2026-10-01T12:00:00Z")
    Instant endTime
) {
}
