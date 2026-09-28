package tech.lokum.parkinglot.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.lokum.parkinglot.entity.Reservation;
import tech.lokum.parkinglot.entity.ReservationStatus;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Reservation} entities.
 */
@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    /**
     * Finds all reservations made by a user.
     */
    List<Reservation> findByUserId(Long userId);

    /**
     * Retrieves a paginated list of reservations for a specific user.
     */
    Page<Reservation> findByUserId(Long userId, Pageable pageable);

    /**
     * Finds a reservation by its ID and user ID to enforce customer data ownership.
     */
    Optional<Reservation> findByIdAndUserId(Long id, Long userId);

    /**
     * Finds reservations for a specific parking spot.
     */
    List<Reservation> findByParkingSpotId(Long parkingSpotId);

    /**
     * Finds all reservations matching a particular lifecycle status.
     */
    List<Reservation> findByStatus(ReservationStatus status);

    /**
     * Retrieves a paginated list of reservations by status.
     */
    Page<Reservation> findByStatus(ReservationStatus status, Pageable pageable);

    /**
     * Finds all active reservations for a given spot that overlap with the requested time interval.
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

    /**
     * Checks if any active reservation exists that overlaps with the requested interval.
     */
    default boolean hasOverlappingReservations(
        Long spotId,
        Instant startTime,
        Instant endTime,
        Collection<ReservationStatus> excludedStatuses
    ) {
        return !findOverlappingReservations(spotId, startTime, endTime, excludedStatuses).isEmpty();
    }

    /**
     * Finds active reservations overlapping an interval on a spot, excluding a specific reservation ID (useful during updates).
     */
    @Query("""
        SELECT r FROM Reservation r
        WHERE r.parkingSpot.id = :spotId
          AND r.id != :reservationId
          AND r.status NOT IN (:excludedStatuses)
          AND r.startTime < :endTime
          AND r.endTime > :startTime
    """)
    List<Reservation> findOverlappingReservationsExcludingId(
        @Param("spotId") Long spotId,
        @Param("reservationId") Long reservationId,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime,
        @Param("excludedStatuses") Collection<ReservationStatus> excludedStatuses
    );

    /**
     * Quick boolean check to determine if any overlapping active reservation exists, excluding a specific reservation ID.
     */
    default boolean hasOverlappingReservationsExcludingId(
        Long spotId,
        Long reservationId,
        Instant startTime,
        Instant endTime,
        Collection<ReservationStatus> excludedStatuses
    ) {
        return !findOverlappingReservationsExcludingId(spotId, reservationId, startTime, endTime, excludedStatuses).isEmpty();
    }

    /**
     * Finds reservations that have expired (pending or confirmed but end_time is before the given timestamp).
     */
    List<Reservation> findByStatusAndEndTimeBefore(ReservationStatus status, Instant beforeTime);
}
