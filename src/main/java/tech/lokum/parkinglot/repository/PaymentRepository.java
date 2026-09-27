package tech.lokum.parkinglot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.lokum.parkinglot.entity.Payment;
import tech.lokum.parkinglot.entity.PaymentStatus;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Payment} entities.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByReservationId(Long reservationId);

    Optional<Payment> findByTransactionId(String transactionId);

    List<Payment> findByStatus(PaymentStatus status);
}
