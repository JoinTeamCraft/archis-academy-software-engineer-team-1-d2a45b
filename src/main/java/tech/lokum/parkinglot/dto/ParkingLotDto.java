package tech.lokum.parkinglot.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tech.lokum.parkinglot.entity.User;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParkingLotDto {

    private Long id;
    private String name;
    private String address;
    private User operator;
}