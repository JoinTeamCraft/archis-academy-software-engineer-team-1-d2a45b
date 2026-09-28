package tech.lokum.parkinglot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.lokum.parkinglot.dto.PaymentResponse;
import tech.lokum.parkinglot.entity.Payment;
import tech.lokum.parkinglot.exception.BadRequestException;
import tech.lokum.parkinglot.exception.ResourceNotFoundException;
import tech.lokum.parkinglot.repository.PaymentRepository;

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
}
