package tech.lokum.parkinglot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.lokum.parkinglot.dto.ErrorResponse;
import tech.lokum.parkinglot.dto.PaymentResponse;
import tech.lokum.parkinglot.service.PaymentService;

/**
 * REST controller for payment queries and billing details.
 */
@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Endpoints for retrieving payment details and transaction history")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
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
}
