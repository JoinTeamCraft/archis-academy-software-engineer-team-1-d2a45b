package tech.lokum.parkinglot.gateway.sandbox;

import org.springframework.stereotype.Component;
import tech.lokum.parkinglot.entity.PaymentStatus;
import tech.lokum.parkinglot.gateway.PaymentCallbackResult;
import tech.lokum.parkinglot.gateway.PaymentGateway;
import tech.lokum.parkinglot.gateway.PaymentGatewayType;
import tech.lokum.parkinglot.gateway.PaymentInitiationRequest;
import tech.lokum.parkinglot.gateway.PaymentInitiationResult;
import tech.lokum.parkinglot.gateway.PaymentVerificationResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * In-memory sandbox payment gateway for testing and offline environments.
 */
@Component
public class SandboxPaymentGateway implements PaymentGateway {

    @Override
    public PaymentGatewayType getGatewayType() {
        return PaymentGatewayType.SANDBOX;
    }

    @Override
    public PaymentInitiationResult initiatePayment(PaymentInitiationRequest request) {
        String txId = "sbx_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String secret = "sbx_secret_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return PaymentInitiationResult.success(txId, secret, "requires_payment_method");
    }

    @Override
    public PaymentVerificationResult verifyPayment(String transactionId) {
        if (transactionId == null || transactionId.isBlank()) {
            return PaymentVerificationResult.failed(transactionId, "Invalid transaction ID");
        }
        return PaymentVerificationResult.success(transactionId, BigDecimal.valueOf(10.00), "USD", Instant.now());
    }

    @Override
    public PaymentCallbackResult processWebhook(String payload, String signatureHeader) {
        return PaymentCallbackResult.success("sandbox.payment.succeeded", "sbx_tx_123", 1L, 1L, PaymentStatus.SUCCESS);
    }
}
