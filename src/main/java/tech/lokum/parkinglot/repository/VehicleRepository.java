package tech.lokum.parkinglot.repository;



import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import tech.lokum.parkinglot.entity.Vehicle;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    //Optional<Vehicle> findByLicensePlate(String licensePlate);
}