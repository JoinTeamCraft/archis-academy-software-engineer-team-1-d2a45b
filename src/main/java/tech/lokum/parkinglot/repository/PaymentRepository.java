package tech.lokum.parkinglot.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.lokum.parkinglot.entity.Payment;
import tech.lokum.parkinglot.entity.PaymentStatus;

import java.time.Instant;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Payment} entities.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Finds payment details by primary key identifier, eagerly fetching reservation details.
     */
    @Query("SELECT p FROM Payment p LEFT JOIN FETCH p.reservation WHERE p.id = :id")
    Optional<Payment> findByIdWithReservation(@Param("id") Long id);

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

    /**
     * Finds payments matching both status and reservation ID.
     */
    List<Payment> findByStatusAndReservationId(PaymentStatus status, Long reservationId);

    /**
     * Finds payments matching both status and reservation ID with pagination.
     */
    Page<Payment> findByStatusAndReservationId(PaymentStatus status, Long reservationId, Pageable pageable);

    /**
     * Retrieves payments based on optional status and reservation ID filters.
     */
    @Query("""
        SELECT p FROM Payment p
        LEFT JOIN FETCH p.reservation r
        WHERE (:status IS NULL OR p.status = :status)
          AND (:reservationId IS NULL OR r.id = :reservationId)
        ORDER BY p.id ASC
    """)
    List<Payment> findPaymentsWithFilters(
        @Param("status") PaymentStatus status,
        @Param("reservationId") Long reservationId
    );

    /**
     * Updates payment status, transaction ID / confirmation number, and paidAt timestamp.
     */
    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE Payment p
        SET p.status = :status,
            p.transactionId = :confirmationNumber,
            p.paidAt = :paidAt
        WHERE p.id = :id
    """)
    int updateStatusAndConfirmation(
        @Param("id") Long id,
        @Param("status") PaymentStatus status,
        @Param("confirmationNumber") String confirmationNumber,
        @Param("paidAt") Instant paidAt
    );

    /**
     * Retrieves payments with reservation details for reporting.
     */
    @Query("""
        SELECT p FROM Payment p
        LEFT JOIN FETCH p.reservation r
        LEFT JOIN FETCH r.user u
        WHERE (:startDate IS NULL OR p.createdAt >= :startDate)
          AND (:endDate IS NULL OR p.createdAt <= :endDate)
        ORDER BY p.createdAt DESC
    """)
    List<Payment> findPaymentsForReport(
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate
    );
}
