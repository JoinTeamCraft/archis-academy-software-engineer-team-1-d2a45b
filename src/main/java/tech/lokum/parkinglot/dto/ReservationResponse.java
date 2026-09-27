package tech.lokum.parkinglot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import tech.lokum.parkinglot.entity.Reservation;
import tech.lokum.parkinglot.entity.ReservationStatus;
import tech.lokum.parkinglot.entity.VehicleType;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO representing parking reservation details returned by the API.
 */
@Schema(description = "Reservation response details")
public record ReservationResponse(
    @Schema(description = "Unique identifier of the reservation", example = "501")
    @JsonProperty("id")
    Long id,

    @Schema(description = "ID of the reserving user", example = "1")
    Long userId,

    @Schema(description = "Email of the reserving user", example = "customer@example.com")
    String userEmail,

    @Schema(description = "Full name of the reserving user", example = "Jane Doe")
    String userName,

    @Schema(description = "ID of the reserved vehicle", example = "201")
    Long vehicleId,

    @Schema(description = "Vehicle license plate", example = "ABC-1234")
    String vehicleLicensePlate,

    @Schema(description = "Vehicle category type", example = "CAR")
    VehicleType vehicleType,

    @Schema(description = "ID of the reserved parking spot", example = "101")
    Long parkingSpotId,

    @Schema(description = "Spot designation number", example = "A-101")
    String spotNumber,

    @Schema(description = "Floor number of the spot", example = "1")
    Integer floorNumber,

    @Schema(description = "ID of the parking lot", example = "1")
    Long parkingLotId,

    @Schema(description = "Name of the parking lot", example = "Downtown Central Garage")
    String parkingLotName,

    @Schema(description = "Scheduled start time in UTC", example = "2026-10-01T10:00:00Z")
    Instant startTime,

    @Schema(description = "Scheduled end time in UTC", example = "2026-10-01T12:00:00Z")
    Instant endTime,

    @Schema(description = "Actual vehicle arrival time in UTC", example = "2026-10-01T10:05:00Z")
    Instant actualEntryTime,

    @Schema(description = "Actual vehicle departure time in UTC", example = "2026-10-01T11:55:00Z")
    Instant actualExitTime,

    @Schema(description = "Calculated total parking charge", example = "20.00")
    BigDecimal totalAmount,

    @Schema(description = "Current lifecycle status of the reservation", example = "CONFIRMED")
    ReservationStatus status,

    @Schema(description = "Reservation creation timestamp in UTC", example = "2026-09-27T12:00:00Z")
    Instant createdAt
) {
    @JsonProperty("reservationId")
    @Schema(description = "Alias reservation ID", example = "501")
    public Long getReservationId() {
        return id;
    }

    public static ReservationResponse fromEntity(Reservation r) {
        if (r == null) {
            return null;
        }

        Long userId = (r.getUser() != null) ? r.getUser().getId() : null;
        String userEmail = (r.getUser() != null) ? r.getUser().getEmail() : null;
        String userName = (r.getUser() != null) ? r.getUser().getFullName() : null;

        Long vehicleId = (r.getVehicle() != null) ? r.getVehicle().getId() : null;
        String licensePlate = (r.getVehicle() != null) ? r.getVehicle().getLicensePlate() : null;
        VehicleType vType = (r.getVehicle() != null) ? r.getVehicle().getVehicleType() : null;

        Long spotId = (r.getParkingSpot() != null) ? r.getParkingSpot().getId() : null;
        String spotNumber = (r.getParkingSpot() != null) ? r.getParkingSpot().getSpotNumber() : null;
        Integer floorNumber = (r.getParkingSpot() != null) ? r.getParkingSpot().getFloorNumber() : null;

        Long lotId = null;
        String lotName = null;
        if (r.getParkingSpot() != null && r.getParkingSpot().getParkingLot() != null) {
            lotId = r.getParkingSpot().getParkingLot().getId();
            lotName = r.getParkingSpot().getParkingLot().getName();
        }

        return new ReservationResponse(
            r.getId(),
            userId,
            userEmail,
            userName,
            vehicleId,
            licensePlate,
            vType,
            spotId,
            spotNumber,
            floorNumber,
            lotId,
            lotName,
            r.getStartTime(),
            r.getEndTime(),
            r.getActualEntryTime(),
            r.getActualExitTime(),
            r.getTotalAmount(),
            r.getStatus(),
            r.getCreatedAt()
        );
    }
}
