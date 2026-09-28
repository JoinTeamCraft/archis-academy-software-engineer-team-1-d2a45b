package tech.lokum.parkinglot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import tech.lokum.parkinglot.entity.Payment;
import tech.lokum.parkinglot.entity.PaymentMethod;
import tech.lokum.parkinglot.entity.PaymentStatus;

import java.math.BigDecimal;

/**
 * Response payload representing the details of a specific payment.
 */
@Schema(description = "Details of a processed or pending payment")
public record PaymentResponse(
    @Schema(description = "Unique identifier of the payment", example = "301")
    Long paymentId,

    @Schema(description = "Unique identifier of the associated reservation", example = "501")
    Long reservationId,

    @Schema(description = "Payment amount", example = "25.00")
    BigDecimal amount,

    @Schema(description = "Payment method used", example = "CREDIT_CARD")
    PaymentMethod paymentMethod,

    @Schema(description = "Currency code for the transaction", example = "USD")
    String currency,

    @Schema(description = "Payment transaction status", example = "SUCCESS")
    PaymentStatus status,

    @Schema(description = "External confirmation or transaction number", example = "STRIPE123456")
    String confirmationNumber
) {
    public static PaymentResponse fromEntity(Payment payment) {
        Long resId = (payment.getReservation() != null) ? payment.getReservation().getId() : null;
        String curr = (payment.getCurrency() != null && !payment.getCurrency().isBlank()) ? payment.getCurrency() : "USD";
        return new PaymentResponse(
            payment.getId(),
            resId,
            payment.getAmount(),
            payment.getPaymentMethod(),
            curr,
            payment.getStatus(),
            payment.getTransactionId()
        );
    }
}
