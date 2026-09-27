package tech.lokum.parkinglot.repository;



import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import tech.lokum.parkinglot.entity.ParkingSpot;
import tech.lokum.parkinglot.entity.SpotType;

public interface ParkingSpotRepository
        extends JpaRepository<ParkingSpot, Long> {

    List<ParkingSpot> findByType(SpotType type);
}