package tech.lokum.parkinglot.notification.model;

import java.time.Instant;

/**
 * Data transfer model representing a notification email to be sent.
 */
public record EmailDetails(
    String to,
    String subject,
    String body,
    String htmlBody,
    Instant sentAt
) {
    public EmailDetails(String to, String subject, String body, String htmlBody) {
        this(to, subject, body, htmlBody, Instant.now());
    }

    public EmailDetails(String to, String subject, String body) {
        this(to, subject, body, null, Instant.now());
    }
}
