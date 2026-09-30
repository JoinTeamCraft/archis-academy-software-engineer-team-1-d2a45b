package tech.lokum.parkinglot.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import tech.lokum.parkinglot.dto.CreateReservationRequest;
import tech.lokum.parkinglot.dto.PaymentNotificationRequest;
import tech.lokum.parkinglot.dto.RegisterRequest;
import tech.lokum.parkinglot.dto.ReservationResponse;
import tech.lokum.parkinglot.entity.ParkingLot;
import tech.lokum.parkinglot.entity.ParkingSpot;
import tech.lokum.parkinglot.entity.Payment;
import tech.lokum.parkinglot.entity.PaymentMethod;
import tech.lokum.parkinglot.entity.PaymentStatus;
import tech.lokum.parkinglot.entity.Reservation;
import tech.lokum.parkinglot.entity.Role;
import tech.lokum.parkinglot.entity.User;
import tech.lokum.parkinglot.entity.Vehicle;
import tech.lokum.parkinglot.entity.VehicleType;
import tech.lokum.parkinglot.notification.model.EmailDetails;
import tech.lokum.parkinglot.notification.service.EmailService;
import tech.lokum.parkinglot.repository.ParkingLotRepository;
import tech.lokum.parkinglot.repository.ParkingSpotRepository;
import tech.lokum.parkinglot.repository.PaymentRepository;
import tech.lokum.parkinglot.repository.ReservationRepository;
import tech.lokum.parkinglot.repository.UserRepository;
import tech.lokum.parkinglot.repository.VehicleRepository;
import tech.lokum.parkinglot.service.PaymentService;
import tech.lokum.parkinglot.service.ReservationService;
import tech.lokum.parkinglot.service.UserService;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class NotificationIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ParkingLotRepository parkingLotRepository;

    @Autowired
    private ParkingSpotRepository parkingSpotRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @BeforeEach
    void setUp() {
        emailService.clearSentEmails();
    }

    @Test
    @DisplayName("User registration should automatically trigger welcome notification email")
    void shouldTriggerEmailOnUserRegistration() {
        RegisterRequest request = new RegisterRequest(
                "notify_user",
                "notify.user@example.com",
                "SecurePassword123!",
                "Notification Test User",
                "+1987654321",
                Role.USER
        );

        userService.registerUser(request);

        List<EmailDetails> sent = emailService.getSentEmails();
        assertThat(sent).isNotEmpty();

        EmailDetails registrationEmail = sent.stream()
                .filter(e -> "notify.user@example.com".equals(e.to()))
                .findFirst()
                .orElse(null);

        assertThat(registrationEmail).isNotNull();
        assertThat(registrationEmail.subject()).contains("Welcome to Parking Lot System");
        assertThat(registrationEmail.body()).contains("notify_user");
        assertThat(registrationEmail.htmlBody()).contains("Notification Test User");
    }

    @Test
    @DisplayName("Reservation creation should automatically trigger booking confirmation notification email")
    void shouldTriggerEmailOnReservationCreation() {
        User user = new User("res_user", "res.user@example.com", "pass12345", "Reservation User", null, Role.USER);
        user = userRepository.save(user);

        Vehicle vehicle = new Vehicle("RES-777", VehicleType.CAR, "Honda", "Civic", "Red", user);
        vehicle = vehicleRepository.save(vehicle);

        ParkingLot lot = new ParkingLot("North Lot", "456 North Blvd", 80, BigDecimal.valueOf(5.00));
        lot = parkingLotRepository.save(lot);

        ParkingSpot spot = new ParkingSpot("N-01", 1, VehicleType.CAR, lot);
        spot = parkingSpotRepository.save(spot);

        Instant start = Instant.now().plus(2, ChronoUnit.HOURS);
        Instant end = start.plus(3, ChronoUnit.HOURS);

        CreateReservationRequest request = new CreateReservationRequest(
                user.getId(),
                vehicle.getId(),
                spot.getId(),
                start,
                end
        );

        ReservationResponse response = reservationService.createReservation(request);
        assertThat(response).isNotNull();

        List<EmailDetails> sent = emailService.getSentEmails();
        assertThat(sent).isNotEmpty();

        EmailDetails bookingEmail = sent.stream()
                .filter(e -> "res.user@example.com".equals(e.to()))
                .findFirst()
                .orElse(null);

        assertThat(bookingEmail).isNotNull();
        assertThat(bookingEmail.subject()).contains("Booking Confirmed - Reservation #" + response.id());
        assertThat(bookingEmail.body())
                .contains("North Lot")
                .contains("N-01")
                .contains("RES-777");
        assertThat(bookingEmail.htmlBody()).contains("N-01");
    }

    @Test
    @DisplayName("Successful payment status notification should trigger payment receipt notification email")
    void shouldTriggerEmailOnPaymentSuccess() {
        User user = new User("pay_user", "pay.user@example.com", "pass12345", "Payment User", null, Role.USER);
        user = userRepository.save(user);

        Vehicle vehicle = new Vehicle("PAY-888", VehicleType.CAR, "BMW", "330i", "Black", user);
        vehicle = vehicleRepository.save(vehicle);

        ParkingLot lot = new ParkingLot("South Lot", "789 South Ave", 50, BigDecimal.valueOf(6.00));
        lot = parkingLotRepository.save(lot);

        ParkingSpot spot = new ParkingSpot("S-10", 1, VehicleType.CAR, lot);
        spot = parkingSpotRepository.save(spot);

        Instant start = Instant.now().plus(1, ChronoUnit.HOURS);
        Instant end = start.plus(2, ChronoUnit.HOURS);

        Reservation reservation = new Reservation(user, vehicle, spot, start, end, BigDecimal.valueOf(12.00));
        reservation = reservationRepository.save(reservation);

        Payment payment = new Payment(reservation, BigDecimal.valueOf(12.00), PaymentMethod.CREDIT_CARD);
        payment = paymentRepository.save(payment);

        PaymentNotificationRequest request = new PaymentNotificationRequest(
                payment.getId(),
                PaymentStatus.SUCCESS,
                "CONF-12345678"
        );

        paymentService.handlePaymentNotification(request);

        List<EmailDetails> sent = emailService.getSentEmails();
        assertThat(sent).isNotEmpty();

        EmailDetails paymentEmail = sent.stream()
                .filter(e -> "pay.user@example.com".equals(e.to()))
                .findFirst()
                .orElse(null);

        assertThat(paymentEmail).isNotNull();
        assertThat(paymentEmail.subject()).contains("Payment Receipt - Transaction #CONF-12345678");
        assertThat(paymentEmail.body())
                .contains("CONF-12345678")
                .contains("$12.00")
                .contains("SUCCESS");
        assertThat(paymentEmail.htmlBody()).contains("CONF-12345678");
    }
}
