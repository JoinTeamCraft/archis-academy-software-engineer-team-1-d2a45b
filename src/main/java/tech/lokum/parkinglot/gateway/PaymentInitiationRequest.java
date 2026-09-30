package tech.lokum.parkinglot.gateway;

import tech.lokum.parkinglot.entity.PaymentMethod;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Data passed to an external gateway to initiate a payment transaction.
 */
public record PaymentInitiationRequest(
    Long paymentId,
    Long reservationId,
    BigDecimal amount,
    String currency,
    PaymentMethod paymentMethod,
    String customerEmail,
    String description,
    Map<String, String> metadata
) {}
