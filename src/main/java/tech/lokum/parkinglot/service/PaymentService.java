package tech.lokum.parkinglot.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tech.lokum.parkinglot.dto.PaymentRequest;
import tech.lokum.parkinglot.dto.PaymentResponse;
import tech.lokum.parkinglot.entity.Payment;
import tech.lokum.parkinglot.entity.PaymentStatus;
import tech.lokum.parkinglot.entity.Reservation;
import tech.lokum.parkinglot.exception.GlobalExceptionHandler.ResourceNotFoundException;
import tech.lokum.parkinglot.repository.PaymentRepository;
import tech.lokum.parkinglot.repository.ReservationRepository;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final PaymentGateway paymentGateway;

    @Transactional
    public PaymentResponse createPayment(
            PaymentRequest request,
            String userEmail) {

        Reservation reservation = reservationRepository.findById(
                request.getReservationId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reservation not found"));

        // Ownership check
        if (!reservation.getUser().getEmail().equals(userEmail)) {
            throw new AccessDeniedException(
                    "You cannot pay for this reservation");
        }

        BigDecimal amount = calculateAmount(reservation);

        Payment payment = new Payment();
        payment.setReservation(reservation);
        payment.setAmount(amount);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(LocalDateTime.now());

        Payment gatewayResult = paymentGateway.process(payment);

        payment.setStatus(gatewayResult.getStatus());
        payment.setTransactionReference(
                gatewayResult.getTransactionReference());
        payment.setPaidAt(gatewayResult.getPaidAt());

        Payment savedPayment = paymentRepository.save(payment);

        return new PaymentResponse(
                savedPayment.getId(),
                savedPayment.getAmount(),
                savedPayment.getStatus(),
                savedPayment.getTransactionReference());
    }

    private BigDecimal calculateAmount(
            Reservation reservation) {

        // TODO: Replace with actual billing calculation.
        return BigDecimal.valueOf(200);
    }
}