package tech.lokum.parkinglot.repository;



import org.springframework.data.jpa.repository.JpaRepository;

import tech.lokum.parkinglot.entity.ParkingLot;

public interface ParkingLotRepository
        extends JpaRepository<ParkingLot, Long> {
}