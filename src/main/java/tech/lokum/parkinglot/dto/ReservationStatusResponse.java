package tech.lokum.parkinglot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import tech.lokum.parkinglot.entity.Reservation;
import tech.lokum.parkinglot.entity.ReservationStatus;

/**
 * Response payload representing the updated status of a parking reservation.
 */
@Schema(description = "Response containing updated reservation status")
public record ReservationStatusResponse(
    @Schema(description = "ID of the reservation", example = "501")
    Long reservationId,

    @Schema(description = "Updated status of the reservation", example = "CANCELLED")
    ReservationStatus status
) {
    public static ReservationStatusResponse fromEntity(Reservation reservation) {
        return new ReservationStatusResponse(reservation.getId(), reservation.getStatus());
    }

    public static ReservationStatusResponse of(Long reservationId, ReservationStatus status) {
        return new ReservationStatusResponse(reservationId, status);
    }
}
