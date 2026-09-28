package tech.lokum.parkinglot.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response payload confirming processing of a payment gateway notification.
 */
@Schema(description = "Payment notification processing response")
public record PaymentNotificationResponse(
    @Schema(description = "Confirmation message", example = "Payment status updated successfully.")
    String message
) {
    public static PaymentNotificationResponse success() {
        return new PaymentNotificationResponse("Payment status updated successfully.");
    }

    public static PaymentNotificationResponse of(String message) {
        return new PaymentNotificationResponse(message);
    }
}
