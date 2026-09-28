package tech.lokum.parkinglot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import tech.lokum.parkinglot.entity.ReservationStatus;

/**
 * Request payload for updating the status of an existing parking reservation.
 */
@Schema(description = "Request body for updating reservation status")
public record UpdateReservationStatusRequest(
    @NotNull(message = "Status is required")
    @Schema(description = "New reservation status", example = "CANCELLED")
    ReservationStatus status
) {
}
