package tech.lokum.parkinglot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.lokum.parkinglot.dto.PaymentResponse;
import tech.lokum.parkinglot.entity.Payment;
import tech.lokum.parkinglot.entity.PaymentStatus;
import tech.lokum.parkinglot.exception.BadRequestException;
import tech.lokum.parkinglot.exception.ResourceNotFoundException;
import tech.lokum.parkinglot.repository.PaymentRepository;

import java.util.List;

/**
 * Service managing payment operations, retrieval, and validation.
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    /**
     * Fetches details of a specific payment by its unique ID.
     *
     * @param paymentId unique payment identifier
     * @return PaymentResponse containing full payment details
     * @throws BadRequestException if paymentId is null or non-positive
     * @throws ResourceNotFoundException if no payment is found for the given ID
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long paymentId) {
        log.debug("Fetching payment details for ID: {}", paymentId);

        if (paymentId == null || paymentId <= 0) {
            throw new BadRequestException("Payment ID must be a positive number");
        }

        Payment payment = paymentRepository.findByIdWithReservation(paymentId)
            .or(() -> paymentRepository.findById(paymentId))
            .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));

        return PaymentResponse.fromEntity(payment);
    }

    /**
     * Retrieves all payments with optional filtering by status and/or reservation ID.
     *
     * @param status optional payment status filter
     * @param reservationId optional reservation ID filter
     * @return list of payment details
     */
    @Transactional(readOnly = true)
    public List<PaymentResponse> getAllPayments(PaymentStatus status, Long reservationId) {
        log.debug("Fetching payments with filters: status={}, reservationId={}", status, reservationId);

        List<Payment> payments = paymentRepository.findPaymentsWithFilters(status, reservationId);

        return payments.stream()
            .map(PaymentResponse::fromEntity)
            .toList();
    }

    /**
     * Retrieves a paginated list of payments with optional filtering by status and/or reservation ID.
     *
     * @param status optional payment status filter
     * @param reservationId optional reservation ID filter
     * @param pageable pagination parameters
     * @return page of payment details
     */
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getAllPayments(PaymentStatus status, Long reservationId, Pageable pageable) {
        log.debug("Fetching payments paginated with filters: status={}, reservationId={}, pageable={}", status, reservationId, pageable);

        Page<Payment> payments;
        if (status != null && reservationId != null) {
            payments = paymentRepository.findByStatusAndReservationId(status, reservationId, pageable);
        } else if (status != null) {
            payments = paymentRepository.findByStatus(status, pageable);
        } else if (reservationId != null) {
            payments = paymentRepository.findByReservationId(reservationId)
                .map(p -> new PageImpl<>(List.of(p), pageable, 1))
                .orElseGet(() -> new PageImpl<>(List.of(), pageable, 0));
        } else {
            payments = paymentRepository.findAll(pageable);
        }

        return payments.map(PaymentResponse::fromEntity);
    }
}
