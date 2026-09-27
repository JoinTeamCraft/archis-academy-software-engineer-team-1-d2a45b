package tech.lokum.parkinglot.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParkingLotRequest {

    private String name;
    private String location;
    private Integer totalSlots;

    // Getters and Setters
}