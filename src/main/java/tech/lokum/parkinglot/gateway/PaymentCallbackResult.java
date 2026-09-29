package tech.lokum.parkinglot.gateway;

import tech.lokum.parkinglot.entity.PaymentStatus;

import java.time.Instant;

/**
 * Result of processing an asynchronous payment callback / webhook from an external gateway.
 */
public record PaymentCallbackResult(
    boolean success,
    String eventType,
    String transactionId,
    Long paymentId,
    Long reservationId,
    PaymentStatus status,
    String message,
    Instant eventTimestamp
) {
    public static PaymentCallbackResult success(
        String eventType,
        String transactionId,
        Long paymentId,
        Long reservationId,
        PaymentStatus status
    ) {
        return new PaymentCallbackResult(
            true,
            eventType,
            transactionId,
            paymentId,
            reservationId,
            status,
            "Webhook event processed successfully",
            Instant.now()
        );
    }

    public static PaymentCallbackResult ignored(String eventType, String message) {
        return new PaymentCallbackResult(
            true,
            eventType,
            null,
            null,
            null,
            null,
            message,
            Instant.now()
        );
    }

    public static PaymentCallbackResult failure(String eventType, String message) {
        return new PaymentCallbackResult(
            false,
            eventType,
            null,
            null,
            null,
            PaymentStatus.FAILED,
            message,
            Instant.now()
        );
    }
}
