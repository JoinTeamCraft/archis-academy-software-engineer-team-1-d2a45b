package tech.lokum.parkinglot.gateway.stripe;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
 * Stripe Payment Gateway integration supporting SAQ-A tokenized PaymentIntents and Webhook events.
 */
@Component
public class StripePaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(StripePaymentGateway.class);

    private final String apiKey;
    private final String webhookSecret;
    private final boolean enabled;
    private final ObjectMapper objectMapper;

    @Autowired
    public StripePaymentGateway(
            @Value("${app.payment.stripe.api-key:sk_test_mock}") String apiKey,
            @Value("${app.payment.stripe.webhook-secret:whsec_test_mock}") String webhookSecret,
            @Value("${app.payment.stripe.enabled:false}") boolean enabled,
            ObjectProvider<ObjectMapper> objectMapperProvider
    ) {
        this.apiKey = apiKey != null ? apiKey.trim() : "sk_test_mock";
        this.webhookSecret = webhookSecret != null ? webhookSecret.trim() : "whsec_test_mock";
        this.enabled = enabled;
        ObjectMapper mapper = objectMapperProvider != null ? objectMapperProvider.getIfAvailable() : null;
        if (mapper == null) {
            mapper = new ObjectMapper();
            mapper.findAndRegisterModules();
        }
        this.objectMapper = mapper;

        if (isLiveStripeConfigured()) {
            Stripe.apiKey = this.apiKey;
            log.info("Stripe Payment Gateway initialized in LIVE mode.");
        } else {
            log.info("Stripe Payment Gateway initialized in SIMULATED/MOCK mode (key: {}, enabled: {}).",
                    maskKey(this.apiKey), this.enabled);
        }
    }

    public StripePaymentGateway(
            String apiKey,
            String webhookSecret,
            boolean enabled,
            ObjectMapper objectMapper
    ) {
        this.apiKey = apiKey != null ? apiKey.trim() : "sk_test_mock";
        this.webhookSecret = webhookSecret != null ? webhookSecret.trim() : "whsec_test_mock";
        this.enabled = enabled;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();

        if (isLiveStripeConfigured()) {
            Stripe.apiKey = this.apiKey;
        }
    }

    @Override
    public PaymentGatewayType getGatewayType() {
        return PaymentGatewayType.STRIPE;
    }

    public boolean isLiveStripeConfigured() {
        return enabled && apiKey != null && !apiKey.isBlank() && !apiKey.startsWith("sk_test_mock");
    }

    @Override
    public PaymentInitiationResult initiatePayment(PaymentInitiationRequest request) {
        if (request == null) {
            return PaymentInitiationResult.failure("Payment initiation request cannot be null");
        }

        BigDecimal amount = request.amount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return PaymentInitiationResult.failure("Payment amount must be greater than zero");
        }

        String currency = (request.currency() != null && !request.currency().isBlank())
                ? request.currency().toLowerCase()
                : "usd";

        if (isLiveStripeConfigured()) {
            try {
                long amountInCents = amount.multiply(BigDecimal.valueOf(100)).longValue();
                PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
                        .setAmount(amountInCents)
                        .setCurrency(currency)
                        .setAutomaticPaymentMethods(
                                PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                        .setEnabled(true)
                                        .build()
                        );

                if (request.customerEmail() != null && !request.customerEmail().isBlank()) {
                    paramsBuilder.setReceiptEmail(request.customerEmail());
                }
                if (request.description() != null && !request.description().isBlank()) {
                    paramsBuilder.setDescription(request.description());
                }

                if (request.paymentId() != null) {
                    paramsBuilder.putMetadata("paymentId", String.valueOf(request.paymentId()));
                }
                if (request.reservationId() != null) {
                    paramsBuilder.putMetadata("reservationId", String.valueOf(request.reservationId()));
                }
                if (request.metadata() != null) {
                    request.metadata().forEach(paramsBuilder::putMetadata);
                }

                PaymentIntent paymentIntent = PaymentIntent.create(paramsBuilder.build());
                return PaymentInitiationResult.success(
                        paymentIntent.getId(),
                        paymentIntent.getClientSecret(),
                        paymentIntent.getStatus()
                );
            } catch (StripeException e) {
                log.error("Stripe API error creating PaymentIntent: {}", e.getMessage(), e);
                return PaymentInitiationResult.failure("Stripe payment initiation failed: " + e.getMessage());
            }
        }

        // Mock / Sandbox mode fallback for tests & local development
        String transactionId = "pi_mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String clientSecret = transactionId + "_secret_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        log.debug("Created simulated Stripe PaymentIntent {} for reservation {}", transactionId, request.reservationId());

        return PaymentInitiationResult.success(transactionId, clientSecret, "requires_payment_method");
    }

    @Override
    public PaymentVerificationResult verifyPayment(String transactionId) {
        if (transactionId == null || transactionId.isBlank()) {
            return PaymentVerificationResult.failed(transactionId, "Transaction ID is required for verification");
        }

        if (isLiveStripeConfigured()) {
            try {
                PaymentIntent paymentIntent = PaymentIntent.retrieve(transactionId);
                String status = paymentIntent.getStatus();
                if ("succeeded".equalsIgnoreCase(status)) {
                    BigDecimal amount = BigDecimal.valueOf(paymentIntent.getAmount()).divide(BigDecimal.valueOf(100));
                    Instant paidAt = paymentIntent.getCreated() != null ? Instant.ofEpochSecond(paymentIntent.getCreated()) : Instant.now();
                    return PaymentVerificationResult.success(transactionId, amount, paymentIntent.getCurrency().toUpperCase(), paidAt);
                } else if ("requires_payment_method".equalsIgnoreCase(status) || "requires_confirmation".equalsIgnoreCase(status) || "processing".equalsIgnoreCase(status)) {
                    return PaymentVerificationResult.pending(transactionId);
                } else {
                    return PaymentVerificationResult.failed(transactionId, "Stripe payment status: " + status);
                }
            } catch (StripeException e) {
                log.error("Stripe API error verifying PaymentIntent {}: {}", transactionId, e.getMessage(), e);
                return PaymentVerificationResult.failed(transactionId, "Stripe verification error: " + e.getMessage());
            }
        }

        // Simulated verification
        if (transactionId.startsWith("pi_mock_failed")) {
            return PaymentVerificationResult.failed(transactionId, "Simulated payment failure");
        }
        if (transactionId.startsWith("pi_mock_pending")) {
            return PaymentVerificationResult.pending(transactionId);
        }

        return PaymentVerificationResult.success(
                transactionId,
                BigDecimal.valueOf(25.00),
                "USD",
                Instant.now()
        );
    }

    @Override
    public PaymentCallbackResult processWebhook(String payload, String signatureHeader) {
        if (payload == null || payload.isBlank()) {
            return PaymentCallbackResult.failure("unknown", "Empty webhook payload received");
        }

        if (isLiveStripeConfigured()) {
            Event event;
            try {
                event = Webhook.constructEvent(payload, signatureHeader, webhookSecret);
            } catch (SignatureVerificationException e) {
                log.error("Stripe webhook signature verification failed: {}", e.getMessage());
                return PaymentCallbackResult.failure("signature_verification", "Invalid signature header");
            } catch (Exception e) {
                log.error("Error parsing Stripe webhook: {}", e.getMessage(), e);
                return PaymentCallbackResult.failure("parse_error", e.getMessage());
            }

            return handleStripeEvent(event, payload);
        }

        // Simulated / Mock webhook handling
        if (signatureHeader != null && signatureHeader.equals("invalid_signature")) {
            return PaymentCallbackResult.failure("signature_verification", "Invalid signature header");
        }

        return parseWebhookPayloadSimulated(payload);
    }

    private PaymentCallbackResult handleStripeEvent(Event event, String rawPayload) {
        String eventType = event.getType();
        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        StripeObject stripeObject = dataObjectDeserializer.getObject().orElse(null);

        if (stripeObject instanceof PaymentIntent paymentIntent) {
            String transactionId = paymentIntent.getId();
            Long paymentId = parseLongSafe(paymentIntent.getMetadata() != null ? paymentIntent.getMetadata().get("paymentId") : null);
            Long reservationId = parseLongSafe(paymentIntent.getMetadata() != null ? paymentIntent.getMetadata().get("reservationId") : null);

            return mapEventToResult(eventType, transactionId, paymentId, reservationId);
        }

        // Fallback to simulated payload parser if StripeObject is empty
        return parseWebhookPayloadSimulated(rawPayload);
    }

    private PaymentCallbackResult parseWebhookPayloadSimulated(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            String eventType = root.path("type").asText("unknown");

            JsonNode dataObject = root.path("data").path("object");
            String transactionId = dataObject.path("id").asText(null);
            JsonNode metadata = dataObject.path("metadata");
            Long paymentId = metadata.has("paymentId") ? metadata.path("paymentId").asLong() : null;
            Long reservationId = metadata.has("reservationId") ? metadata.path("reservationId").asLong() : null;

            return mapEventToResult(eventType, transactionId, paymentId, reservationId);
        } catch (Exception e) {
            log.error("Error parsing simulated webhook JSON payload: {}", e.getMessage());
            return PaymentCallbackResult.failure("parse_error", "Failed to parse webhook JSON: " + e.getMessage());
        }
    }

    private PaymentCallbackResult mapEventToResult(
            String eventType,
            String transactionId,
            Long paymentId,
            Long reservationId
    ) {
        switch (eventType) {
            case "payment_intent.succeeded" -> {
                return PaymentCallbackResult.success(
                        eventType,
                        transactionId,
                        paymentId,
                        reservationId,
                        PaymentStatus.SUCCESS
                );
            }
            case "payment_intent.payment_failed", "charge.failed" -> {
                return PaymentCallbackResult.success(
                        eventType,
                        transactionId,
                        paymentId,
                        reservationId,
                        PaymentStatus.FAILED
                );
            }
            default -> {
                return PaymentCallbackResult.ignored(eventType, "Ignored unhandled Stripe event type: " + eventType);
            }
        }
    }

    private Long parseLongSafe(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String maskKey(String key) {
        if (key == null || key.length() <= 8) {
            return "***";
        }
        return key.substring(0, 7) + "..." + key.substring(key.length() - 4);
    }
}
