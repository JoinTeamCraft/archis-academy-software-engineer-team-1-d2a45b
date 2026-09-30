package tech.lokum.parkinglot.gateway;

/**
 * Result returned by an external gateway upon initiating payment.
 */
public record PaymentInitiationResult(
    boolean success,
    String transactionId,
    String clientSecret,
    String checkoutUrl,
    String status,
    String message
) {
    public static PaymentInitiationResult success(String transactionId, String clientSecret, String status) {
        return new PaymentInitiationResult(true, transactionId, clientSecret, null, status, "Payment initiated successfully");
    }

    public static PaymentInitiationResult failure(String message) {
        return new PaymentInitiationResult(false, null, null, null, "FAILED", message);
    }
}
