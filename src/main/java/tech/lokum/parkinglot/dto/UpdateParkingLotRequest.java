package tech.lokum.parkinglot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * DTO payload for updating an existing parking lot.
 */
@Schema(description = "Request body for updating an existing parking lot")
public record UpdateParkingLotRequest(
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Schema(description = "Updated name of the parking lot", example = "Downtown Central Garage Updated")
    String name,

    @Size(min = 3, max = 255, message = "Location must be between 3 and 255 characters")
    @Schema(description = "Updated physical location or address", example = "125 Main St, Metro City")
    String location,

    @Min(value = 1, message = "Capacity must be at least 1")
    @Schema(description = "Updated capacity of the parking lot", example = "200")
    Integer capacity,

    @Schema(description = "Active status of the parking lot", example = "true")
    Boolean active
) {
}
