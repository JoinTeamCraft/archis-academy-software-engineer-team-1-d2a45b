package tech.lokum.parkinglot.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tech.lokum.parkinglot.entity.ParkingLot;
import tech.lokum.parkinglot.entity.ParkingSpot;
import tech.lokum.parkinglot.entity.Payment;
import tech.lokum.parkinglot.entity.PaymentMethod;
import tech.lokum.parkinglot.entity.PaymentStatus;
import tech.lokum.parkinglot.entity.Reservation;
import tech.lokum.parkinglot.entity.ReservationStatus;
import tech.lokum.parkinglot.entity.Role;
import tech.lokum.parkinglot.entity.User;
import tech.lokum.parkinglot.entity.Vehicle;
import tech.lokum.parkinglot.entity.VehicleType;
import tech.lokum.parkinglot.notification.model.EmailDetails;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class EmailTemplateServiceTest {

    private EmailTemplateService templateService;

    @BeforeEach
    void setUp() {
        templateService = new EmailTemplateService();
    }

    @Test
    @DisplayName("buildUserRegistrationEmail should include user name, username, email, and role")
    void shouldBuildUserRegistrationEmail() {
        User user = new User("alice_w", "alice@example.com", "hash", "Alice Wonderland", "+123456789", Role.USER);
        user.setId(10L);

        EmailDetails details = templateService.buildUserRegistrationEmail(user);

        assertThat(details.to()).isEqualTo("alice@example.com");
        assertThat(details.subject()).contains("Welcome to Parking Lot System");
        assertThat(details.body()).contains("Alice Wonderland")
                .contains("alice_w")
                .contains("alice@example.com")
                .contains("USER");
        assertThat(details.htmlBody()).contains("Alice Wonderland")
                .contains("alice_w")
                .contains("alice@example.com");
    }

    @Test
    @DisplayName("buildBookingConfirmationEmail should include reservation ID, lot name, spot, vehicle, and price")
    void shouldBuildBookingConfirmationEmail() {
        User user = new User("bob_m", "bob@example.com", "hash", "Bob Marley", null, Role.USER);
        user.setId(20L);

        Vehicle vehicle = new Vehicle("CAR-9999", VehicleType.CAR, "Tesla", "Model 3", "White", user);
        ParkingLot lot = new ParkingLot("Downtown Garage", "123 Main St", 100);
        ParkingSpot spot = new ParkingSpot("D-14", 1, VehicleType.CAR, lot);

        Instant start = Instant.now().plus(1, ChronoUnit.HOURS);
        Instant end = Instant.now().plus(4, ChronoUnit.HOURS);
        Reservation reservation = new Reservation(user, vehicle, spot, start, end, BigDecimal.valueOf(35.50), ReservationStatus.CONFIRMED);
        reservation.setId(101L);

        EmailDetails details = templateService.buildBookingConfirmationEmail(reservation);

        assertThat(details.to()).isEqualTo("bob@example.com");
        assertThat(details.subject()).contains("Booking Confirmed - Reservation #101");
        assertThat(details.body()).contains("Bob Marley")
                .contains("#101")
                .contains("Downtown Garage")
                .contains("D-14")
                .contains("CAR-9999")
                .contains("$35.50");
        assertThat(details.htmlBody()).contains("D-14")
                .contains("Downtown Garage")
                .contains("$35.50");
    }

    @Test
    @DisplayName("buildPaymentReceiptEmail should include transaction ID, amount, payment method, and reservation ID")
    void shouldBuildPaymentReceiptEmail() {
        User user = new User("carol_d", "carol@example.com", "hash", "Carol Danvers", null, Role.USER);
        user.setId(30L);

        Vehicle vehicle = new Vehicle("FLY-0001", VehicleType.CAR, "Audi", "e-tron", "Blue", user);
        ParkingLot lot = new ParkingLot("Sky Lot", "777 High St", 50);
        ParkingSpot spot = new ParkingSpot("S-01", 1, VehicleType.CAR, lot);

        Instant start = Instant.now();
        Instant end = start.plus(2, ChronoUnit.HOURS);
        Reservation reservation = new Reservation(user, vehicle, spot, start, end, BigDecimal.valueOf(25.00), ReservationStatus.CONFIRMED);
        reservation.setId(202L);

        Payment payment = new Payment(reservation, BigDecimal.valueOf(25.00), PaymentMethod.CREDIT_CARD, PaymentStatus.SUCCESS, "TX-99887766", Instant.now());
        payment.setId(505L);

        EmailDetails details = templateService.buildPaymentReceiptEmail(payment);

        assertThat(details.to()).isEqualTo("carol@example.com");
        assertThat(details.subject()).contains("Payment Receipt - Transaction #TX-99887766");
        assertThat(details.body()).contains("Carol Danvers")
                .contains("#505")
                .contains("TX-99887766")
                .contains("#202")
                .contains("$25.00 USD")
                .contains("CREDIT_CARD")
                .contains("SUCCESS");
        assertThat(details.htmlBody()).contains("TX-99887766")
                .contains("$25.00 USD");
    }
}
