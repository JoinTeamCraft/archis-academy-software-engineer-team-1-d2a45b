package tech.lokum.parkinglot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import tech.lokum.parkinglot.entity.ParkingLot;

/**
 * DTO representing parking lot details returned by the API.
 */
@Schema(description = "Parking lot response object")
public record ParkingLotResponse(
    @Schema(description = "Unique identifier of the parking lot", example = "1")
    Long id,

    @Schema(description = "Name of the parking lot", example = "Downtown Central Garage")
    String name,

    @Schema(description = "Physical address or location description", example = "123 Main St, Metro City")
    String location,

    @Schema(description = "Total parking spot capacity", example = "150")
    Integer capacity
) {
    public static ParkingLotResponse fromEntity(ParkingLot entity) {
        return new ParkingLotResponse(
            entity.getId(),
            entity.getName(),
            entity.getLocation(),
            entity.getCapacity()
        );
    }
}
