package tech.lokum.parkinglot.notification.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import tech.lokum.parkinglot.notification.event.BookingConfirmationEvent;
import tech.lokum.parkinglot.notification.event.PaymentReceiptEvent;
import tech.lokum.parkinglot.notification.event.UserRegistrationEvent;
import tech.lokum.parkinglot.notification.service.EmailService;

/**
 * Event listener that catches domain events and dispatches asynchronous/decoupled email notifications.
 */
@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final EmailService emailService;

    public NotificationEventListener(EmailService emailService) {
        this.emailService = emailService;
    }

    /**
     * Handles user registration event by triggering welcome email.
     */
    @EventListener
    public void handleUserRegistration(UserRegistrationEvent event) {
        if (event == null || event.user() == null) {
            return;
        }
        log.info("Received UserRegistrationEvent for user: {}", event.user().getEmail());
        emailService.sendUserRegistrationEmail(event.user());
    }

    /**
     * Handles booking confirmation event by triggering booking details email.
     */
    @EventListener
    public void handleBookingConfirmation(BookingConfirmationEvent event) {
        if (event == null || event.reservation() == null) {
            return;
        }
        log.info("Received BookingConfirmationEvent for reservation ID: {}", event.reservation().getId());
        emailService.sendBookingConfirmationEmail(event.reservation());
    }

    /**
     * Handles payment receipt event by triggering payment receipt email.
     */
    @EventListener
    public void handlePaymentReceipt(PaymentReceiptEvent event) {
        if (event == null || event.payment() == null) {
            return;
        }
        log.info("Received PaymentReceiptEvent for payment ID: {}", event.payment().getId());
        emailService.sendPaymentReceiptEmail(event.payment());
    }
}
