package tech.lokum.parkinglot.notification.event;

import tech.lokum.parkinglot.entity.Payment;

/**
 * Event published when a payment is completed successfully.
 */
public record PaymentReceiptEvent(Payment payment) {
}
