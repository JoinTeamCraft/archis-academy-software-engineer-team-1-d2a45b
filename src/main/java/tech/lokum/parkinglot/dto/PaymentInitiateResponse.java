package tech.lokum.parkinglot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import tech.lokum.parkinglot.entity.PaymentMethod;
import tech.lokum.parkinglot.entity.PaymentStatus;
import tech.lokum.parkinglot.gateway.PaymentGatewayType;

import java.math.BigDecimal;

/**
 * Response returned after payment initiation, including token/clientSecret for SAQ-A compliant frontend completion.
 */
@Schema(description = "Details returned after payment initiation")
public record PaymentInitiateResponse(
    @Schema(description = "Unique ID of the payment record", example = "301")
    Long paymentId,

    @Schema(description = "Associated reservation ID", example = "501")
    Long reservationId,

    @Schema(description = "Payment amount", example = "25.00")
    BigDecimal amount,

    @Schema(description = "Currency code", example = "USD")
    String currency,

    @Schema(description = "Current payment status", example = "PENDING")
    PaymentStatus status,

    @Schema(description = "Payment method used", example = "CREDIT_CARD")
    PaymentMethod paymentMethod,

    @Schema(description = "Payment gateway provider", example = "STRIPE")
    PaymentGatewayType gatewayType,

    @Schema(description = "External transaction or PaymentIntent ID", example = "pi_3MtwBwLkdIwHu7ix28a3tqPa")
    String transactionId,

    @Schema(description = "Client secret token for Stripe Elements or digital wallet completion", example = "pi_3Mtw..._secret_...")
    String clientSecret,

    @Schema(description = "Descriptive message or instructions", example = "Payment initiated successfully")
    String message
) {}
