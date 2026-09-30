package tech.lokum.parkinglot.gateway;

import tech.lokum.parkinglot.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Verification details retrieved directly from external payment gateway.
 */
public record PaymentVerificationResult(
    boolean success,
    PaymentStatus status,
    String transactionId,
    BigDecimal amount,
    String currency,
    Instant paidAt,
    String failureMessage
) {
    public static PaymentVerificationResult success(String transactionId, BigDecimal amount, String currency, Instant paidAt) {
        return new PaymentVerificationResult(
            true,
            PaymentStatus.SUCCESS,
            transactionId,
            amount,
            currency,
            paidAt != null ? paidAt : Instant.now(),
            null
        );
    }

    public static PaymentVerificationResult pending(String transactionId) {
        return new PaymentVerificationResult(
            true,
            PaymentStatus.PENDING,
            transactionId,
            null,
            null,
            null,
            null
        );
    }

    public static PaymentVerificationResult failed(String transactionId, String failureMessage) {
        return new PaymentVerificationResult(
            false,
            PaymentStatus.FAILED,
            transactionId,
            null,
            null,
            null,
            failureMessage
        );
    }
}
