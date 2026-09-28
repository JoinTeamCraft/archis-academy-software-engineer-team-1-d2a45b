package tech.lokum.parkinglot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import tech.lokum.parkinglot.entity.PaymentStatus;

/**
 * Webhook / notification payload received from an external payment gateway.
 */
@Schema(description = "Payload for payment gateway status notification")
public record PaymentNotificationRequest(
    @NotNull(message = "Payment ID is required")
    @Schema(description = "ID of the payment to update", example = "301")
    Long paymentId,

    @NotNull(message = "Status is required")
    @Schema(description = "Updated payment status", example = "SUCCESS")
    PaymentStatus status,

    @Schema(description = "External gateway confirmation or transaction reference", example = "STRIPE123456")
    String confirmationNumber
) {
}
