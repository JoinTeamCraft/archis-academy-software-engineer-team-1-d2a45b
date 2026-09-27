package tech.lokum.parkinglot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ParkingSpot {

    @Id
    @GeneratedValue
    private Long id;

    private String spotNumber;

    @Enumerated(EnumType.STRING)
    private SpotType type;

    @ManyToOne
    @JoinColumn(name = "parking_lot_id")
    private ParkingLot parkingLot;

    private boolean active;
}