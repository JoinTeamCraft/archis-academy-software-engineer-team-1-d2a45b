package tech.lokum.parkinglot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import tech.lokum.parkinglot.entity.PaymentMethod;
import tech.lokum.parkinglot.gateway.PaymentGatewayType;

import java.math.BigDecimal;

/**
 * Request payload to initiate an external payment for a reservation.
 */
@Schema(description = "Payload to initiate a secure external gateway payment")
public record PaymentInitiateRequest(
    @NotNull(message = "Reservation ID is required")
    @Schema(description = "ID of the reservation to be paid", example = "501")
    Long reservationId,

    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    @Schema(description = "Payment amount. If omitted, uses reservation total fee", example = "25.00")
    BigDecimal amount,

    @Schema(description = "ISO-4217 Currency code", example = "USD", defaultValue = "USD")
    String currency,

    @Schema(description = "Payment method", example = "CREDIT_CARD", defaultValue = "CREDIT_CARD")
    PaymentMethod paymentMethod,

    @Schema(description = "Payment gateway provider", example = "STRIPE", defaultValue = "STRIPE")
    PaymentGatewayType gatewayType,

    @Schema(description = "Customer email for receipt", example = "user@example.com")
    String customerEmail,

    @Schema(description = "Payment description or notes", example = "Reservation #501 parking fee")
    String description
) {
    public String getEffectiveCurrency() {
        return (currency != null && !currency.isBlank()) ? currency.toUpperCase() : "USD";
    }

    public PaymentMethod getEffectivePaymentMethod() {
        return paymentMethod != null ? paymentMethod : PaymentMethod.CREDIT_CARD;
    }

    public PaymentGatewayType getEffectiveGatewayType() {
        return gatewayType != null ? gatewayType : PaymentGatewayType.STRIPE;
    }
}
