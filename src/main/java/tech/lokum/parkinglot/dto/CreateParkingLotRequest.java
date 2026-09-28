package tech.lokum.parkinglot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO payload for creating a new parking lot.
 */
@Schema(description = "Request body for creating a new parking lot")
public record CreateParkingLotRequest(
    @NotBlank(message = "Parking lot name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Schema(description = "Name of the parking lot", example = "Downtown Central Garage")
    String name,

    @NotBlank(message = "Location is required")
    @Size(min = 3, max = 255, message = "Location must be between 3 and 255 characters")
    @Schema(description = "Physical location or address", example = "123 Main St, Metro City")
    String location,

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    @Schema(description = "Total capacity of the parking lot", example = "150")
    Integer capacity
) {
}
