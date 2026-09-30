package tech.lokum.parkinglot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tech.lokum.parkinglot.dto.ErrorResponse;
import tech.lokum.parkinglot.dto.PaymentInitiateRequest;
import tech.lokum.parkinglot.dto.PaymentInitiateResponse;
import tech.lokum.parkinglot.dto.PaymentNotificationRequest;
import tech.lokum.parkinglot.dto.PaymentNotificationResponse;
import tech.lokum.parkinglot.dto.PaymentResponse;
import tech.lokum.parkinglot.dto.PaymentWebhookResponse;
import tech.lokum.parkinglot.entity.PaymentStatus;
import tech.lokum.parkinglot.service.PaymentService;

import java.util.List;

/**
 * REST controller for payment queries, gateway initiation, verification, and webhooks.
 */
@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Endpoints for payment initiation, status checks, verification, and external gateway webhooks")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Initiates a payment session with an external payment gateway (e.g., Stripe, PayPal).
     *
     * @param request initiation payload
     * @return 201 Created with client secret token for front-end completion
     */
    @PostMapping("/initiate")
    @Operation(
        summary = "Initiate external gateway payment",
        description = "Initializes a payment intent or transaction with an external gateway (e.g. Stripe) and returns tokenized client secret."
    )
    @ApiResponse(
        responseCode = "201",
        description = "Payment initiated successfully",
        content = @Content(schema = @Schema(implementation = PaymentInitiateResponse.class))
    )
    @ApiResponse(
        responseCode = "400",
        description = "Invalid payment initiation parameters",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @ApiResponse(
        responseCode = "404",
        description = "Reservation not found",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    public ResponseEntity<PaymentInitiateResponse> initiatePayment(
        @Valid @RequestBody PaymentInitiateRequest request
    ) {
        PaymentInitiateResponse response = paymentService.initiatePayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Verifies payment status directly with the gateway and synchronizes the internal payment record.
     *
     * @param paymentId unique payment identifier
     * @return 200 OK with refreshed payment details
     */
    @PostMapping("/{paymentId}/verify")
    @Operation(
        summary = "Verify payment status with gateway",
        description = "Queries the external payment gateway to verify and synchronize the latest payment state."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Payment status verified successfully",
        content = @Content(schema = @Schema(implementation = PaymentResponse.class))
    )
    @ApiResponse(
        responseCode = "400",
        description = "Invalid payment ID supplied",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @ApiResponse(
        responseCode = "404",
        description = "Payment not found",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    public ResponseEntity<PaymentResponse> verifyPayment(
        @Parameter(description = "ID of the payment to verify", example = "301")
        @PathVariable Long paymentId
    ) {
        PaymentResponse response = paymentService.verifyPayment(paymentId);
        return ResponseEntity.ok(response);
    }

    /**
     * Receives and processes asynchronous webhook callbacks from external payment gateways (e.g. Stripe).
     *
     * @param stripeSignature cryptographic signature header from Stripe
     * @param payload raw webhook payload body
     * @return 200 OK with processing confirmation
     */
    @PostMapping("/webhook")
    @Operation(
        summary = "External gateway webhook callback",
        description = "Receives signed webhook notifications from external payment gateways to record asynchronous payment completions."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Webhook received and processed",
        content = @Content(schema = @Schema(implementation = PaymentWebhookResponse.class))
    )
    public ResponseEntity<PaymentWebhookResponse> handleWebhook(
        @RequestHeader(value = "Stripe-Signature", required = false) String stripeSignature,
        @RequestBody String payload
    ) {
        PaymentWebhookResponse response = paymentService.handleWebhook(payload, stripeSignature);
        return ResponseEntity.ok(response);
    }

    /**
     * Fetches details of a specific payment by its identifier.
     *
     * @param paymentId unique payment identifier
     * @return 200 OK with payment details
     */
    @GetMapping("/{paymentId}")
    @Operation(
        summary = "Fetch payment details by ID",
        description = "Retrieves the financial, reservation, and status details of a specific payment by its identifier."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Payment details successfully retrieved",
        content = @Content(schema = @Schema(implementation = PaymentResponse.class))
    )
    @ApiResponse(
        responseCode = "400",
        description = "Invalid payment ID supplied",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @ApiResponse(
        responseCode = "404",
        description = "Payment not found",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    public ResponseEntity<PaymentResponse> getPaymentById(
        @Parameter(description = "ID of the payment to retrieve", example = "301")
        @PathVariable Long paymentId
    ) {
        PaymentResponse response = paymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(response);
    }

    /**
     * Lists all payments, with optional filters by payment status or reservation ID.
     *
     * @param status optional status filter
     * @param reservationId optional reservation ID filter
     * @param page optional zero-based page index
     * @param size optional page size
     * @return 200 OK with list of payment details
     */
    @GetMapping
    @Operation(
        summary = "List all payments with optional filters",
        description = "Retrieves a list of payments, optionally filtered by payment status (e.g. SUCCESS, PENDING) or reservation ID, with optional pagination."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Payments successfully retrieved",
        content = @Content(array = @ArraySchema(schema = @Schema(implementation = PaymentResponse.class)))
    )
    public ResponseEntity<List<PaymentResponse>> getAllPayments(
        @Parameter(description = "Optional filter by payment status", example = "SUCCESS")
        @RequestParam(name = "status", required = false) PaymentStatus status,
        @Parameter(description = "Optional filter by reservation ID", example = "501")
        @RequestParam(name = "reservationId", required = false) Long reservationId,
        @Parameter(description = "Zero-based page index")
        @RequestParam(name = "page", required = false) Integer page,
        @Parameter(description = "Page size")
        @RequestParam(name = "size", required = false) Integer size
    ) {
        if (page != null || size != null) {
            int pageIndex = (page != null && page >= 0) ? page : 0;
            int pageSize = (size != null && size > 0) ? size : 20;
            Pageable pageable = PageRequest.of(pageIndex, pageSize);

            Page<PaymentResponse> paymentPage = paymentService.getAllPayments(status, reservationId, pageable);

            HttpHeaders headers = new HttpHeaders();
            headers.add("X-Total-Count", String.valueOf(paymentPage.getTotalElements()));
            headers.add("X-Total-Pages", String.valueOf(paymentPage.getTotalPages()));
            headers.add("X-Current-Page", String.valueOf(paymentPage.getNumber()));
            headers.add("X-Page-Size", String.valueOf(paymentPage.getSize()));

            return ResponseEntity.ok().headers(headers).body(paymentPage.getContent());
        }

        List<PaymentResponse> payments = paymentService.getAllPayments(status, reservationId);
        return ResponseEntity.ok(payments);
    }

    /**
     * Receives and processes payment status notifications from external payment gateways.
     *
     * @param request notification payload containing paymentId, status, and confirmationNumber
     * @return 200 OK with confirmation message
     */
    @PostMapping("/notifications")
    @Operation(
        summary = "Handle payment status notification",
        description = "Processes status updates and confirmation numbers from external payment gateways (e.g. Stripe, PayPal)."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Payment status updated successfully.",
        content = @Content(schema = @Schema(implementation = PaymentNotificationResponse.class))
    )
    @ApiResponse(
        responseCode = "400",
        description = "Validation failed for notification payload",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @ApiResponse(
        responseCode = "404",
        description = "Payment not found",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    public ResponseEntity<PaymentNotificationResponse> handlePaymentNotification(
        @Valid @RequestBody PaymentNotificationRequest request
    ) {
        PaymentNotificationResponse response = paymentService.handlePaymentNotification(request);
        return ResponseEntity.ok(response);
    }
}
