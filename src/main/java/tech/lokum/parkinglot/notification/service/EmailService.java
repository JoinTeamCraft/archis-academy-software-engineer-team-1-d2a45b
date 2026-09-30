package tech.lokum.parkinglot.notification.service;

import tech.lokum.parkinglot.entity.Payment;
import tech.lokum.parkinglot.entity.Reservation;
import tech.lokum.parkinglot.entity.User;
import tech.lokum.parkinglot.notification.model.EmailDetails;

import java.util.List;

/**
 * Service managing email delivery operations and notification triggers.
 */
public interface EmailService {

    /**
     * Sends an email notification using structured EmailDetails.
     */
    void sendEmail(EmailDetails emailDetails);

    /**
     * Sends a simple plain text email.
     */
    void sendSimpleEmail(String to, String subject, String text);

    /**
     * Sends an HTML formatted email with plain text fallback.
     */
    void sendHtmlEmail(String to, String subject, String htmlBody, String textFallback);

    /**
     * Dispatches a user registration welcome email.
     */
    void sendUserRegistrationEmail(User user);

    /**
     * Dispatches a booking confirmation email.
     */
    void sendBookingConfirmationEmail(Reservation reservation);

    /**
     * Dispatches a payment receipt confirmation email.
     */
    void sendPaymentReceiptEmail(Payment payment);

    /**
     * Retrieves sent emails history (useful for integration testing and delivery audits).
     */
    List<EmailDetails> getSentEmails();

    /**
     * Clears sent email delivery history.
     */
    void clearSentEmails();
}
