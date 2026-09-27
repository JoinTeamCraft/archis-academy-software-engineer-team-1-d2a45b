package tech.lokum.parkinglot.entity;

/**
 * Transaction status for reservation payments.
 */
public enum PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED,
    REFUNDED
}
