package tech.lokum.parkinglot.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tech.lokum.parkinglot.entity.PaymentMethod;
import tech.lokum.parkinglot.entity.PaymentStatus;
import tech.lokum.parkinglot.gateway.stripe.StripePaymentGateway;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StripePaymentGatewayTest {

    private StripePaymentGateway gateway;

    @BeforeEach
    void setUp() {
        gateway = new StripePaymentGateway("sk_test_mock", "whsec_test_mock", false, new ObjectMapper());
    }

    @Test
    @DisplayName("initiatePayment should generate mock transactionId and clientSecret in simulated mode")
    void initiatePaymentSimulated() {
        PaymentInitiationRequest request = new PaymentInitiationRequest(
                100L,
                200L,
                BigDecimal.valueOf(25.00),
                "USD",
                PaymentMethod.CREDIT_CARD,
                "user@example.com",
                "Reservation payment",
                Map.of("key", "val")
        );

        PaymentInitiationResult result = gateway.initiatePayment(request);

        assertThat(result.success()).isTrue();
        assertThat(result.transactionId()).startsWith("pi_mock_");
        assertThat(result.clientSecret()).contains(result.transactionId());
        assertThat(result.status()).isEqualTo("requires_payment_method");
    }

    @Test
    @DisplayName("initiatePayment should reject null or zero amount")
    void initiatePaymentInvalidAmount() {
        PaymentInitiationRequest request = new PaymentInitiationRequest(
                100L,
                200L,
                BigDecimal.ZERO,
                "USD",
                PaymentMethod.CREDIT_CARD,
                "user@example.com",
                "Reservation payment",
                null
        );

        PaymentInitiationResult result = gateway.initiatePayment(request);
        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("greater than zero");
    }

    @Test
    @DisplayName("verifyPayment should return SUCCESS for standard mock transaction ID")
    void verifyPaymentSuccess() {
        PaymentVerificationResult result = gateway.verifyPayment("pi_mock_1234567890abcdef");

        assertThat(result.success()).isTrue();
        assertThat(result.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(result.transactionId()).isEqualTo("pi_mock_1234567890abcdef");
    }

    @Test
    @DisplayName("verifyPayment should return PENDING for pending mock transaction ID")
    void verifyPaymentPending() {
        PaymentVerificationResult result = gateway.verifyPayment("pi_mock_pending_xyz");

        assertThat(result.success()).isTrue();
        assertThat(result.status()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    @DisplayName("verifyPayment should return FAILED for failed mock transaction ID")
    void verifyPaymentFailed() {
        PaymentVerificationResult result = gateway.verifyPayment("pi_mock_failed_xyz");

        assertThat(result.success()).isFalse();
        assertThat(result.status()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    @DisplayName("processWebhook should process payment_intent.succeeded payload")
    void processWebhookSucceeded() {
        String payload = """
                {
                    "type": "payment_intent.succeeded",
                    "data": {
                        "object": {
                            "id": "pi_mock_webhook_99",
                            "status": "succeeded",
                            "metadata": {
                                "paymentId": "50",
                                "reservationId": "75"
                            }
                        }
                    }
                }
                """;

        PaymentCallbackResult result = gateway.processWebhook(payload, "test_sig");

        assertThat(result.success()).isTrue();
        assertThat(result.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(result.transactionId()).isEqualTo("pi_mock_webhook_99");
        assertThat(result.paymentId()).isEqualTo(50L);
        assertThat(result.reservationId()).isEqualTo(75L);
    }

    @Test
    @DisplayName("processWebhook should process payment_intent.payment_failed payload")
    void processWebhookFailed() {
        String payload = """
                {
                    "type": "payment_intent.payment_failed",
                    "data": {
                        "object": {
                            "id": "pi_mock_webhook_fail",
                            "status": "failed",
                            "metadata": {
                                "paymentId": "51",
                                "reservationId": "76"
                            }
                        }
                    }
                }
                """;

        PaymentCallbackResult result = gateway.processWebhook(payload, "test_sig");

        assertThat(result.success()).isTrue();
        assertThat(result.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(result.transactionId()).isEqualTo("pi_mock_webhook_fail");
    }

    @Test
    @DisplayName("processWebhook should ignore unsupported event types gracefully")
    void processWebhookIgnored() {
        String payload = """
                {
                    "type": "customer.created",
                    "data": {
                        "object": {
                            "id": "cus_123"
                        }
                    }
                }
                """;

        PaymentCallbackResult result = gateway.processWebhook(payload, "test_sig");

        assertThat(result.success()).isTrue();
        assertThat(result.status()).isNull();
        assertThat(result.message()).contains("Ignored unhandled Stripe event");
    }
}
