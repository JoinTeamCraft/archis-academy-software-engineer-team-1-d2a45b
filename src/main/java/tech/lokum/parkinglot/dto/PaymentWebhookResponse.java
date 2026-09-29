package tech.lokum.parkinglot.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Standard confirmation response returned for incoming gateway webhooks.
 */
@Schema(description = "Confirmation response for webhook callback processing")
public record PaymentWebhookResponse(
    @Schema(description = "Whether the webhook was received and processed successfully", example = "true")
    boolean received,

    @Schema(description = "Status of webhook processing", example = "processed")
    String status,

    @Schema(description = "Message or event summary", example = "Webhook event payment_intent.succeeded handled")
    String message
) {
    public static PaymentWebhookResponse processed(String message) {
        return new PaymentWebhookResponse(true, "processed", message);
    }

    public static PaymentWebhookResponse ignored(String message) {
        return new PaymentWebhookResponse(true, "ignored", message);
    }

    public static PaymentWebhookResponse failed(String message) {
        return new PaymentWebhookResponse(false, "failed", message);
    }
}
