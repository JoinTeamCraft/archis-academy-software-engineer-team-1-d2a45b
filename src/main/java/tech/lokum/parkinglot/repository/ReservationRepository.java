package tech.lokum.parkinglot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.lokum.parkinglot.entity.Reservation;
import tech.lokum.parkinglot.entity.ReservationStatus;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 * Spring Data JPA repository for {@link Reservation} entities.
 */
@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByUserId(Long userId);

    List<Reservation> findByParkingSpotId(Long parkingSpotId);

    List<Reservation> findByStatus(ReservationStatus status);

    /**
     * Finds existing active reservations for a spot that overlap with the requested time interval.
     */
    @Query("""
        SELECT r FROM Reservation r
        WHERE r.parkingSpot.id = :spotId
          AND r.status NOT IN (:excludedStatuses)
          AND r.startTime < :endTime
          AND r.endTime > :startTime
    """)
    List<Reservation> findOverlappingReservations(
        @Param("spotId") Long spotId,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime,
        @Param("excludedStatuses") Collection<ReservationStatus> excludedStatuses
    );
}
