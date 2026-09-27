package tech.lokum.parkinglot.repository;



import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import tech.lokum.parkinglot.entity.Reservation;
import tech.lokum.parkinglot.entity.ReservationStatus;

public interface ReservationRepository
        extends JpaRepository<Reservation, Long> {

    List<Reservation> findByStatus(
            ReservationStatus status);
}