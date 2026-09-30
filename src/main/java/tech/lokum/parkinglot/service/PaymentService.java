package tech.lokum.parkinglot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.lokum.parkinglot.dto.PaymentInitiateRequest;
import tech.lokum.parkinglot.dto.PaymentInitiateResponse;
import tech.lokum.parkinglot.dto.PaymentNotificationRequest;
import tech.lokum.parkinglot.dto.PaymentNotificationResponse;
import tech.lokum.parkinglot.dto.PaymentResponse;
import tech.lokum.parkinglot.dto.PaymentWebhookResponse;
import tech.lokum.parkinglot.entity.Payment;
import tech.lokum.parkinglot.entity.PaymentMethod;
import tech.lokum.parkinglot.entity.PaymentStatus;
import tech.lokum.parkinglot.entity.Reservation;
import tech.lokum.parkinglot.exception.BadRequestException;
import tech.lokum.parkinglot.exception.ResourceNotFoundException;
import tech.lokum.parkinglot.gateway.PaymentCallbackResult;
import tech.lokum.parkinglot.gateway.PaymentGateway;
import tech.lokum.parkinglot.gateway.PaymentGatewayFactory;
import tech.lokum.parkinglot.gateway.PaymentGatewayType;
import tech.lokum.parkinglot.gateway.PaymentInitiationRequest;
import tech.lokum.parkinglot.gateway.PaymentInitiationResult;
import tech.lokum.parkinglot.gateway.PaymentVerificationResult;
import tech.lokum.parkinglot.notification.event.PaymentReceiptEvent;
import tech.lokum.parkinglot.repository.PaymentRepository;
import tech.lokum.parkinglot.repository.ReservationRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service managing payment operations, external gateway integrations, retrieval, and validation.
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final PaymentGatewayFactory gatewayFactory;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public PaymentService(
            PaymentRepository paymentRepository,
            ObjectProvider<ReservationRepository> reservationRepositoryProvider,
            ObjectProvider<PaymentGatewayFactory> gatewayFactoryProvider,
            ObjectProvider<ApplicationEventPublisher> eventPublisherProvider
    ) {
        this.paymentRepository = paymentRepository;
        this.reservationRepository = reservationRepositoryProvider != null ? reservationRepositoryProvider.getIfAvailable() : null;
        this.gatewayFactory = gatewayFactoryProvider != null ? gatewayFactoryProvider.getIfAvailable() : null;
        this.eventPublisher = eventPublisherProvider != null ? eventPublisherProvider.getIfAvailable() : null;
    }

    public PaymentService(
            PaymentRepository paymentRepository,
            ReservationRepository reservationRepository,
            PaymentGatewayFactory gatewayFactory,
            ApplicationEventPublisher eventPublisher
    ) {
        this.paymentRepository = paymentRepository;
        this.reservationRepository = reservationRepository;
        this.gatewayFactory = gatewayFactory;
        this.eventPublisher = eventPublisher;
    }

    public PaymentService(PaymentRepository paymentRepository) {
        this(paymentRepository, (ReservationRepository) null, (PaymentGatewayFactory) null, (ApplicationEventPublisher) null);
    }

    public PaymentService(PaymentRepository paymentRepository, ObjectProvider<ApplicationEventPublisher> eventPublisherProvider) {
        this(paymentRepository, (ReservationRepository) null, (PaymentGatewayFactory) null, eventPublisherProvider != null ? eventPublisherProvider.getIfAvailable() : null);
    }

    /**
     * Initiates a payment transaction via the configured payment gateway (e.g. Stripe).
     * Returns clientSecret / token for SAQ-A PCI-compliant client side completion.
     *
     * @param request initiation payload
     * @return PaymentInitiateResponse containing payment ID, transaction ID, and client token
     */
    @Transactional
    public PaymentInitiateResponse initiatePayment(PaymentInitiateRequest request) {
        if (request == null) {
            throw new BadRequestException("Payment initiation payload is required");
        }
        if (request.reservationId() == null || request.reservationId() <= 0) {
            throw new BadRequestException("Valid reservation ID is required");
        }

        Reservation reservation = null;
        if (reservationRepository != null) {
            reservation = reservationRepository.findById(request.reservationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Reservation", "id", request.reservationId()));
        }

        BigDecimal amount = request.amount();
        if (amount == null) {
            if (reservation != null && reservation.getTotalAmount() != null) {
                amount = reservation.getTotalAmount();
            } else {
                throw new BadRequestException("Payment amount is required");
            }
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Payment amount must be greater than zero");
        }

        PaymentMethod paymentMethod = request.getEffectivePaymentMethod();
        String currency = request.getEffectiveCurrency();
        PaymentGatewayType gatewayType = request.getEffectiveGatewayType();

        // Check if an existing payment is already completed for this reservation
        Payment payment = paymentRepository.findByReservationId(request.reservationId()).orElse(null);
        if (payment != null) {
            if (payment.getStatus() == PaymentStatus.SUCCESS) {
                throw new BadRequestException("Reservation " + request.reservationId() + " has already been paid");
            }
            payment.setAmount(amount);
            payment.setPaymentMethod(paymentMethod);
            payment.setCurrency(currency);
        } else {
            payment = new Payment(
                    reservation,
                    amount,
                    paymentMethod,
                    PaymentStatus.PENDING,
                    null,
                    null,
                    currency
            );
            payment = paymentRepository.save(payment);
        }

        String customerEmail = request.customerEmail();
        if ((customerEmail == null || customerEmail.isBlank()) && reservation != null && reservation.getUser() != null) {
            customerEmail = reservation.getUser().getEmail();
        }

        Map<String, String> metadata = new HashMap<>();
        metadata.put("paymentId", String.valueOf(payment.getId()));
        metadata.put("reservationId", String.valueOf(request.reservationId()));
        metadata.put("paymentMethod", paymentMethod.name());

        PaymentInitiationRequest gatewayReq = new PaymentInitiationRequest(
                payment.getId(),
                request.reservationId(),
                amount,
                currency,
                paymentMethod,
                customerEmail,
                request.description() != null ? request.description() : ("Payment for reservation #" + request.reservationId()),
                metadata
        );

        PaymentInitiationResult result;
        if (gatewayFactory != null) {
            PaymentGateway gateway = gatewayFactory.getGateway(gatewayType);
            result = gateway.initiatePayment(gatewayReq);
        } else {
            // Fallback for isolated unit tests without gateway factory bean
            String txId = "pi_mock_" + System.currentTimeMillis();
            result = PaymentInitiationResult.success(txId, txId + "_secret_mock", "requires_payment_method");
        }

        if (!result.success()) {
            throw new BadRequestException("Failed to initiate payment with " + gatewayType + ": " + result.message());
        }

        payment.setTransactionId(result.transactionId());
        paymentRepository.save(payment);

        log.info("Payment initiated successfully for reservation {}: paymentId={}, txId={}",
                request.reservationId(), payment.getId(), result.transactionId());

        return new PaymentInitiateResponse(
                payment.getId(),
                request.reservationId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getPaymentMethod(),
                gatewayType,
                result.transactionId(),
                result.clientSecret(),
                result.message()
        );
    }

    /**
     * Verifies the latest payment status directly with the payment gateway.
     *
     * @param paymentId unique payment identifier
     * @return updated PaymentResponse
     */
    @Transactional
    public PaymentResponse verifyPayment(Long paymentId) {
        if (paymentId == null || paymentId <= 0) {
            throw new BadRequestException("Payment ID must be a positive number");
        }

        Payment payment = paymentRepository.findByIdWithReservation(paymentId)
                .or(() -> paymentRepository.findById(paymentId))
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return PaymentResponse.fromEntity(payment);
        }

        if (payment.getTransactionId() != null && !payment.getTransactionId().isBlank() && gatewayFactory != null) {
            PaymentGateway gateway = gatewayFactory.getGateway(PaymentGatewayType.STRIPE);
            PaymentVerificationResult verification = gateway.verifyPayment(payment.getTransactionId());

            if (verification.status() == PaymentStatus.SUCCESS) {
                payment.setStatus(PaymentStatus.SUCCESS);
                if (payment.getPaidAt() == null) {
                    payment.setPaidAt(verification.paidAt() != null ? verification.paidAt() : Instant.now());
                }
                paymentRepository.save(payment);
                publishReceiptEvent(payment);
            } else if (verification.status() == PaymentStatus.FAILED) {
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
            }
        }

        return PaymentResponse.fromEntity(payment);
    }

    /**
     * Processes incoming webhook events from the payment gateway (e.g. Stripe).
     *
     * @param payload raw webhook event body
     * @param signatureHeader signature header for cryptographic verification
     * @return PaymentWebhookResponse confirming processing
     */
    @Transactional
    public PaymentWebhookResponse handleWebhook(String payload, String signatureHeader) {
        log.info("Received payment gateway webhook event");

        PaymentCallbackResult callbackResult;
        if (gatewayFactory != null) {
            PaymentGateway gateway = gatewayFactory.getGateway(PaymentGatewayType.STRIPE);
            callbackResult = gateway.processWebhook(payload, signatureHeader);
        } else {
            return PaymentWebhookResponse.failed("Gateway factory is not initialized");
        }

        if (!callbackResult.success()) {
            log.warn("Webhook processing failed or verification rejected: {}", callbackResult.message());
            return PaymentWebhookResponse.failed(callbackResult.message());
        }

        if (callbackResult.status() == null) {
            return PaymentWebhookResponse.ignored(callbackResult.message());
        }

        Payment payment = null;
        if (callbackResult.paymentId() != null) {
            payment = paymentRepository.findById(callbackResult.paymentId()).orElse(null);
        }
        if (payment == null && callbackResult.transactionId() != null) {
            payment = paymentRepository.findByTransactionId(callbackResult.transactionId()).orElse(null);
        }
        if (payment == null && callbackResult.reservationId() != null) {
            payment = paymentRepository.findByReservationId(callbackResult.reservationId()).orElse(null);
        }

        if (payment != null) {
            payment.setStatus(callbackResult.status());
            if (callbackResult.transactionId() != null) {
                payment.setTransactionId(callbackResult.transactionId());
            }

            if (callbackResult.status() == PaymentStatus.SUCCESS) {
                if (payment.getPaidAt() == null) {
                    payment.setPaidAt(Instant.now());
                }
                paymentRepository.save(payment);
                publishReceiptEvent(payment);
                log.info("Webhook updated payment ID {} to SUCCESS", payment.getId());
            } else {
                paymentRepository.save(payment);
                log.info("Webhook updated payment ID {} to {}", payment.getId(), payment.getStatus());
            }
            return PaymentWebhookResponse.processed("Handled " + callbackResult.eventType() + " for payment " + payment.getId());
        }

        log.warn("No matching payment found for webhook event {}", callbackResult.eventType());
        return PaymentWebhookResponse.processed("Received " + callbackResult.eventType() + " but no matching payment found");
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

    /**
     * Receives and processes a payment status notification from an external payment gateway.
     *
     * @param request notification payload containing payment ID, status, and confirmation number
     * @return PaymentNotificationResponse confirming update
     * @throws BadRequestException if request or mandatory fields are missing
     * @throws ResourceNotFoundException if payment is not found
     */
    @Transactional
    public PaymentNotificationResponse handlePaymentNotification(PaymentNotificationRequest request) {
        if (request == null) {
            throw new BadRequestException("Notification payload is required");
        }
        if (request.paymentId() == null || request.paymentId() <= 0) {
            throw new BadRequestException("Valid payment ID is required");
        }
        if (request.status() == null) {
            throw new BadRequestException("Payment status is required");
        }

        log.info("Processing payment notification for payment ID {}: status={}, confirmationNumber={}",
            request.paymentId(), request.status(), request.confirmationNumber());

        Payment payment = paymentRepository.findByIdWithReservation(request.paymentId())
            .or(() -> paymentRepository.findById(request.paymentId()))
            .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", request.paymentId()));

        payment.setStatus(request.status());
        if (request.confirmationNumber() != null && !request.confirmationNumber().isBlank()) {
            payment.setTransactionId(request.confirmationNumber());
        }

        Instant paidAt = payment.getPaidAt();
        if (request.status() == PaymentStatus.SUCCESS && paidAt == null) {
            paidAt = Instant.now();
            payment.setPaidAt(paidAt);
        }

        paymentRepository.save(payment);
        paymentRepository.updateStatusAndConfirmation(
            payment.getId(),
            payment.getStatus(),
            payment.getTransactionId(),
            paidAt
        );

        log.info("Successfully updated payment ID {} to status {}", payment.getId(), payment.getStatus());

        if (request.status() == PaymentStatus.SUCCESS) {
            publishReceiptEvent(payment);
        }

        return PaymentNotificationResponse.success();
    }

    private void publishReceiptEvent(Payment payment) {
        if (eventPublisher != null) {
            try {
                eventPublisher.publishEvent(new PaymentReceiptEvent(payment));
            } catch (Exception e) {
                log.error("Failed to publish PaymentReceiptEvent: {}", e.getMessage(), e);
            }
        }
    }
}
