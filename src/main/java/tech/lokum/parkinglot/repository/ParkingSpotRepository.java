package tech.lokum.parkinglot.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.lokum.parkinglot.entity.ParkingSpot;
import tech.lokum.parkinglot.entity.SpotStatus;
import tech.lokum.parkinglot.entity.VehicleType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link ParkingSpot} entities.
 */
@Repository
public interface ParkingSpotRepository extends JpaRepository<ParkingSpot, Long> {

    /**
     * Finds all spots within a given parking lot.
     */
    List<ParkingSpot> findByParkingLotId(Long parkingLotId);

    /**
     * Retrieves a paginated list of spots within a given parking lot.
     */
    Page<ParkingSpot> findByParkingLotId(Long parkingLotId, Pageable pageable);

    /**
     * Finds spots in a parking lot with a given operational status.
     */
    List<ParkingSpot> findByParkingLotIdAndStatus(Long parkingLotId, SpotStatus status);

    /**
     * Finds spots in a parking lot filtered by vehicle type and status.
     */
    List<ParkingSpot> findByParkingLotIdAndSpotTypeAndStatus(Long parkingLotId, VehicleType spotType, SpotStatus status);

    /**
     * Finds a spot by lot ID and spot number.
     */
    Optional<ParkingSpot> findByParkingLotIdAndSpotNumber(Long parkingLotId, String spotNumber);

    /**
     * Checks if a spot number already exists in a given parking lot.
     */
    boolean existsByParkingLotIdAndSpotNumber(Long parkingLotId, String spotNumber);

    /**
     * Counts spots in a parking lot matching a specific status.
     */
    long countByParkingLotIdAndStatus(Long parkingLotId, SpotStatus status);

    /**
     * Counts total spots in a parking lot.
     */
    long countByParkingLotId(Long parkingLotId);

    /**
     * Finds all spots in a parking lot that fit the vehicle type, have status AVAILABLE,
     * and have NO active or pending reservations overlapping the given [startTime, endTime] window.
     */
    @Query("""
        SELECT s FROM ParkingSpot s
        WHERE s.parkingLot.id = :parkingLotId
          AND s.spotType = :spotType
          AND s.status = tech.lokum.parkinglot.entity.SpotStatus.AVAILABLE
          AND s.id NOT IN (
              SELECT r.parkingSpot.id FROM Reservation r
              WHERE r.parkingSpot.parkingLot.id = :parkingLotId
                AND r.status NOT IN (tech.lokum.parkinglot.entity.ReservationStatus.CANCELLED, tech.lokum.parkinglot.entity.ReservationStatus.EXPIRED)
                AND r.startTime < :endTime
                AND r.endTime > :startTime
          )
        ORDER BY s.floorNumber ASC, s.spotNumber ASC
    """)
    List<ParkingSpot> findAvailableSpotsForTimeWindow(
        @Param("parkingLotId") Long parkingLotId,
        @Param("spotType") VehicleType spotType,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );
}
