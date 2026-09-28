package tech.lokum.parkinglot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.lokum.parkinglot.entity.ParkingSpot;
import tech.lokum.parkinglot.entity.SpotStatus;
import tech.lokum.parkinglot.entity.VehicleType;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link ParkingSpot} entities.
 */
@Repository
public interface ParkingSpotRepository extends JpaRepository<ParkingSpot, Long> {

    List<ParkingSpot> findByParkingLotId(Long parkingLotId);

    List<ParkingSpot> findByParkingLotIdAndStatus(Long parkingLotId, SpotStatus status);

    List<ParkingSpot> findByParkingLotIdAndSpotTypeAndStatus(Long parkingLotId, VehicleType spotType, SpotStatus status);

    Optional<ParkingSpot> findByParkingLotIdAndSpotNumber(Long parkingLotId, String spotNumber);

    boolean existsByParkingLotIdAndSpotNumber(Long parkingLotId, String spotNumber);
}
