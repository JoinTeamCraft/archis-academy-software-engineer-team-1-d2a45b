package tech.lokum.parkinglot.gateway;

/**
 * Common abstraction for external payment gateway integrations (e.g. Stripe, PayPal, Sandbox).
 */
public interface PaymentGateway {

    /**
     * Identifies the gateway provider type.
     */
    PaymentGatewayType getGatewayType();

    /**
     * Initiates a payment transaction and retrieves client token/secret for secure front-end completion.
     */
    PaymentInitiationResult initiatePayment(PaymentInitiationRequest request);

    /**
     * Checks or synchronizes the current payment status directly with the gateway.
     */
    PaymentVerificationResult verifyPayment(String transactionId);

    /**
     * Validates and processes an asynchronous callback/webhook from the gateway.
     */
    PaymentCallbackResult processWebhook(String payload, String signatureHeader);
}
