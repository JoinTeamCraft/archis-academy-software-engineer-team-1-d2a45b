package tech.lokum.parkinglot.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tech.lokum.parkinglot.dto.PaymentInitiateRequest;
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
import tech.lokum.parkinglot.repository.ParkingLotRepository;
import tech.lokum.parkinglot.repository.ParkingSpotRepository;
import tech.lokum.parkinglot.repository.PaymentRepository;
import tech.lokum.parkinglot.repository.ReservationRepository;
import tech.lokum.parkinglot.repository.UserRepository;
import tech.lokum.parkinglot.repository.VehicleRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PaymentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private ParkingLotRepository parkingLotRepository;

    @Autowired
    private ParkingSpotRepository parkingSpotRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    private Reservation reservation;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(new User("paytest@example.com", "SecurePass123!", "Payment Tester", "1234567890", Role.CUSTOMER));

        Vehicle vehicle = vehicleRepository.save(new Vehicle("TEST-PAY", VehicleType.CAR, "Toyota", "Corolla", "Silver", testUser));

        ParkingLot lot = parkingLotRepository.save(new ParkingLot("Payment Test Lot", "100 Gateway Way", 30));

        ParkingSpot spot = parkingSpotRepository.save(new ParkingSpot("PAY-01", 1, VehicleType.CAR, lot));

        Instant now = Instant.now();
        reservation = reservationRepository.save(new Reservation(
                testUser,
                vehicle,
                spot,
                now,
                now.plus(2, ChronoUnit.HOURS),
                BigDecimal.valueOf(20.00),
                ReservationStatus.PENDING
        ));
    }

    @Test
    @DisplayName("POST /api/payments/initiate should create payment and return clientSecret")
    void initiatePaymentSuccess() throws Exception {
        PaymentInitiateRequest request = new PaymentInitiateRequest(
                reservation.getId(),
                BigDecimal.valueOf(20.00),
                "USD",
                PaymentMethod.CREDIT_CARD,
                PaymentGatewayType.STRIPE,
                testUser.getEmail(),
                "Parking payment test"
        );

        mockMvc.perform(post("/api/payments/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reservationId").value(reservation.getId()))
                .andExpect(jsonPath("$.amount").value(20.00))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.transactionId").exists())
                .andExpect(jsonPath("$.clientSecret").exists())
                .andExpect(jsonPath("$.gatewayType").value("STRIPE"));

        Payment savedPayment = paymentRepository.findByReservationId(reservation.getId()).orElse(null);
        assertThat(savedPayment).isNotNull();
        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(savedPayment.getTransactionId()).isNotBlank();
    }

    @Test
    @DisplayName("POST /api/payments/initiate should support DIGITAL_WALLET payment method")
    void initiatePaymentDigitalWallet() throws Exception {
        PaymentInitiateRequest request = new PaymentInitiateRequest(
                reservation.getId(),
                BigDecimal.valueOf(20.00),
                "USD",
                PaymentMethod.DIGITAL_WALLET,
                PaymentGatewayType.STRIPE,
                testUser.getEmail(),
                "Apple Pay test"
        );

        mockMvc.perform(post("/api/payments/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentMethod").value("DIGITAL_WALLET"))
                .andExpect(jsonPath("$.clientSecret").exists());
    }

    @Test
    @DisplayName("POST /api/payments/{id}/verify should verify transaction with gateway and mark SUCCESS")
    void verifyPaymentSuccess() throws Exception {
        Payment payment = new Payment(
                reservation,
                BigDecimal.valueOf(20.00),
                PaymentMethod.CREDIT_CARD,
                PaymentStatus.PENDING,
                "pi_mock_verify_123",
                null,
                "USD"
        );
        payment = paymentRepository.save(payment);

        mockMvc.perform(post("/api/payments/{id}/verify", payment.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(payment.getId()))
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        Payment updated = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(updated.getPaidAt()).isNotNull();
    }

    @Test
    @DisplayName("POST /api/payments/webhook should process payment_intent.succeeded without authentication")
    void webhookPaymentSucceeded() throws Exception {
        Payment payment = new Payment(
                reservation,
                BigDecimal.valueOf(20.00),
                PaymentMethod.CREDIT_CARD,
                PaymentStatus.PENDING,
                "pi_mock_webhook_target",
                null,
                "USD"
        );
        payment = paymentRepository.save(payment);

        String webhookPayload = """
                {
                    "type": "payment_intent.succeeded",
                    "data": {
                        "object": {
                            "id": "pi_mock_webhook_target",
                            "status": "succeeded",
                            "metadata": {
                                "paymentId": "%d",
                                "reservationId": "%d"
                            }
                        }
                    }
                }
                """.formatted(payment.getId(), reservation.getId());

        mockMvc.perform(post("/api/payments/webhook")
                        .header("Stripe-Signature", "t=123,v1=mock_signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(webhookPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.received").value(true))
                .andExpect(jsonPath("$.status").value("processed"));

        Payment updated = paymentRepository.findById(payment.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(updated.getPaidAt()).isNotNull();
    }

    @Test
    @DisplayName("POST /api/payments/initiate should return 400 if reservation is already paid")
    void initiateAlreadyPaidFails() throws Exception {
        Payment payment = new Payment(
                reservation,
                BigDecimal.valueOf(20.00),
                PaymentMethod.CREDIT_CARD,
                PaymentStatus.SUCCESS,
                "pi_mock_already_paid",
                Instant.now(),
                "USD"
        );
        paymentRepository.save(payment);

        PaymentInitiateRequest request = new PaymentInitiateRequest(
                reservation.getId(),
                BigDecimal.valueOf(20.00),
                "USD",
                PaymentMethod.CREDIT_CARD,
                PaymentGatewayType.STRIPE,
                testUser.getEmail(),
                "Duplicate payment attempt"
        );

        mockMvc.perform(post("/api/payments/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Reservation " + reservation.getId() + " has already been paid"));
    }

    @Test
    @DisplayName("POST /api/payments/initiate should return 404 for nonexistent reservation")
    void initiateNonexistentReservationFails() throws Exception {
        PaymentInitiateRequest request = new PaymentInitiateRequest(
                99999L,
                BigDecimal.valueOf(20.00),
                "USD",
                PaymentMethod.CREDIT_CARD,
                PaymentGatewayType.STRIPE,
                testUser.getEmail(),
                "Nonexistent reservation"
        );

        mockMvc.perform(post("/api/payments/initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }
}
