package tech.lokum.parkinglot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.lokum.parkinglot.entity.SpotType;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParkingSpotDto {

    private Long id;
    private String spotNumber;
    private SpotType type;
    private Long parkingLotId;
    private boolean active;
}