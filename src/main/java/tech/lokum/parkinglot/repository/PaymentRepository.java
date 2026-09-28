package tech.lokum.parkinglot.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /**
     * Finds the payment associated with a given reservation.
     */
    Optional<Payment> findByReservationId(Long reservationId);

    /**
     * Finds a payment by its external transaction ID.
     */
    Optional<Payment> findByTransactionId(String transactionId);

    /**
     * Checks if a payment with the given transaction ID exists.
     */
    boolean existsByTransactionId(String transactionId);

    /**
     * Finds all payments matching a status.
     */
    List<Payment> findByStatus(PaymentStatus status);

    /**
     * Retrieves a paginated list of payments matching a status.
     */
    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);

    /**
     * Finds all payments for reservations belonging to a specific user.
     */
    List<Payment> findByReservationUserId(Long userId);
}
