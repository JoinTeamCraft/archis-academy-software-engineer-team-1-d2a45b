package tech.lokum.parkinglot.notification.listener;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.lokum.parkinglot.entity.Payment;
import tech.lokum.parkinglot.entity.Reservation;
import tech.lokum.parkinglot.entity.Role;
import tech.lokum.parkinglot.entity.User;
import tech.lokum.parkinglot.notification.event.BookingConfirmationEvent;
import tech.lokum.parkinglot.notification.event.PaymentReceiptEvent;
import tech.lokum.parkinglot.notification.event.UserRegistrationEvent;
import tech.lokum.parkinglot.notification.service.EmailService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private NotificationEventListener listener;

    @Test
    @DisplayName("handleUserRegistration should trigger sendUserRegistrationEmail")
    void shouldHandleUserRegistrationEvent() {
        User user = new User("test_user", "test@example.com", "secret", "Test User", null, Role.USER);
        UserRegistrationEvent event = new UserRegistrationEvent(user);

        listener.handleUserRegistration(event);

        verify(emailService).sendUserRegistrationEmail(user);
    }

    @Test
    @DisplayName("handleBookingConfirmation should trigger sendBookingConfirmationEmail")
    void shouldHandleBookingConfirmationEvent() {
        Reservation reservation = mock(Reservation.class);
        BookingConfirmationEvent event = new BookingConfirmationEvent(reservation);

        listener.handleBookingConfirmation(event);

        verify(emailService).sendBookingConfirmationEmail(reservation);
    }

    @Test
    @DisplayName("handlePaymentReceipt should trigger sendPaymentReceiptEmail")
    void shouldHandlePaymentReceiptEvent() {
        Payment payment = mock(Payment.class);
        PaymentReceiptEvent event = new PaymentReceiptEvent(payment);

        listener.handlePaymentReceipt(event);

        verify(emailService).sendPaymentReceiptEmail(payment);
    }
}
