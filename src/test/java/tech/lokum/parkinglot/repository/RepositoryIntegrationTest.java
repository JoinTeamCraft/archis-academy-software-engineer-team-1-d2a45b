package tech.lokum.parkinglot.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import tech.lokum.parkinglot.config.JpaConfig;
import tech.lokum.parkinglot.entity.ParkingLot;
import tech.lokum.parkinglot.entity.ParkingSpot;
import tech.lokum.parkinglot.entity.Payment;
import tech.lokum.parkinglot.entity.PaymentMethod;
import tech.lokum.parkinglot.entity.PaymentStatus;
import tech.lokum.parkinglot.entity.Reservation;
import tech.lokum.parkinglot.entity.ReservationStatus;
import tech.lokum.parkinglot.entity.Role;
import tech.lokum.parkinglot.entity.SpotStatus;
import tech.lokum.parkinglot.entity.User;
import tech.lokum.parkinglot.entity.Vehicle;
import tech.lokum.parkinglot.entity.VehicleType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaConfig.class)
class RepositoryIntegrationTest {

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

    private User customer;
    private Vehicle vehicle;
    private ParkingLot lot;
    private ParkingSpot spot1;
    private ParkingSpot spot2;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAllInBatch();
        reservationRepository.deleteAllInBatch();
        parkingSpotRepository.deleteAllInBatch();
        vehicleRepository.deleteAllInBatch();
        parkingLotRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        customer = userRepository.save(new User("john.doe@example.com", "hashpass", "John Doe", "555-1234", Role.CUSTOMER));
        vehicle = vehicleRepository.save(new Vehicle("ABC-9876", VehicleType.CAR, "Honda", "Civic", "Blue", customer));

        lot = parkingLotRepository.save(new ParkingLot("Grand Central", "42nd St, New York", 100, BigDecimal.valueOf(10.00)));
        spot1 = parkingSpotRepository.save(new ParkingSpot("A-01", 1, VehicleType.CAR, lot));
        spot2 = parkingSpotRepository.save(new ParkingSpot("A-02", 1, VehicleType.CAR, lot));
    }

    @Test
    @DisplayName("UserRepository should query users by email case-insensitively and by role")
    void testUserRepository() {
        Optional<User> found = userRepository.findByEmailIgnoreCase("JOHN.DOE@EXAMPLE.COM");
        assertThat(found).isPresent();
        assertThat(found.get().getFullName()).isEqualTo("John Doe");

        assertThat(userRepository.existsByEmailIgnoreCase("john.doe@example.com")).isTrue();
        assertThat(userRepository.existsByEmailIgnoreCase("unknown@example.com")).isFalse();

        List<User> customers = userRepository.findByRoleAndActiveTrue(Role.CUSTOMER);
        assertThat(customers).hasSize(1);
    }

    @Test
    @DisplayName("ParkingLotRepository should support location search and pagination")
    void testParkingLotRepository() {
        List<ParkingLot> foundLots = parkingLotRepository.findByLocationContainingIgnoreCaseAndActiveTrue("42nd");
        assertThat(foundLots).hasSize(1);
        assertThat(foundLots.get(0).getName()).isEqualTo("Grand Central");

        Page<ParkingLot> page = parkingLotRepository.findByActiveTrue(PageRequest.of(0, 10));
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("ParkingSpotRepository should find available spots excluding overlapping reservations")
    void testParkingSpotRepositoryAvailability() {
        Instant now = Instant.now().plus(1, ChronoUnit.HOURS);
        Instant start = now;
        Instant end = now.plus(2, ChronoUnit.HOURS);

        // Create an active reservation for spot1 during [start, end]
        reservationRepository.save(new Reservation(
            customer, vehicle, spot1, start, end, BigDecimal.valueOf(20.00), ReservationStatus.CONFIRMED
        ));

        // When searching for available spots in [start, end], spot1 should be excluded, and spot2 should be available!
        List<ParkingSpot> availableSpots = parkingSpotRepository.findAvailableSpotsForTimeWindow(
            lot.getId(), VehicleType.CAR, start.plus(15, ChronoUnit.MINUTES), end.minus(15, ChronoUnit.MINUTES)
        );

        assertThat(availableSpots)
            .extracting(ParkingSpot::getSpotNumber)
            .contains("A-02")
            .doesNotContain("A-01");
    }

    @Test
    @DisplayName("ReservationRepository should detect overlapping reservations and exclude cancelled/adjacent ones")
    void testReservationOverlapDetection() {
        Instant base = Instant.now().plus(5, ChronoUnit.HOURS);
        Instant resStart = base;
        Instant resEnd = base.plus(2, ChronoUnit.HOURS);

        Reservation existingRes = reservationRepository.save(new Reservation(
            customer, vehicle, spot1, resStart, resEnd, BigDecimal.valueOf(20.00), ReservationStatus.CONFIRMED
        ));

        Set<ReservationStatus> excluded = Set.of(ReservationStatus.CANCELLED, ReservationStatus.EXPIRED);

        // Overlapping request: [resStart + 30m, resEnd + 30m]
        boolean hasOverlap = reservationRepository.hasOverlappingReservations(
            spot1.getId(), resStart.plus(30, ChronoUnit.MINUTES), resEnd.plus(30, ChronoUnit.MINUTES), excluded
        );
        assertThat(hasOverlap).isTrue();

        // Adjacent request before: [resStart - 1h, resStart] -> NO overlap
        boolean adjacentBefore = reservationRepository.hasOverlappingReservations(
            spot1.getId(), resStart.minus(1, ChronoUnit.HOURS), resStart, excluded
        );
        assertThat(adjacentBefore).isFalse();

        // Cancel the reservation -> now overlap should NOT be detected!
        existingRes.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(existingRes);

        boolean overlapAfterCancellation = reservationRepository.hasOverlappingReservations(
            spot1.getId(), resStart.plus(30, ChronoUnit.MINUTES), resEnd.plus(30, ChronoUnit.MINUTES), excluded
        );
        assertThat(overlapAfterCancellation).isFalse();
    }

    @Test
    @DisplayName("PaymentRepository should find payment by reservation ID and transaction ID")
    void testPaymentRepository() {
        Instant start = Instant.now().plus(1, ChronoUnit.HOURS);
        Instant end = Instant.now().plus(2, ChronoUnit.HOURS);
        Reservation res = reservationRepository.save(new Reservation(
            customer, vehicle, spot2, start, end, BigDecimal.valueOf(10.00), ReservationStatus.CONFIRMED
        ));

        Payment payment = paymentRepository.save(new Payment(
            res, BigDecimal.valueOf(10.00), PaymentMethod.CREDIT_CARD, PaymentStatus.SUCCESS, "TX-999000", Instant.now()
        ));

        Optional<Payment> byResId = paymentRepository.findByReservationId(res.getId());
        assertThat(byResId).isPresent();
        assertThat(byResId.get().getTransactionId()).isEqualTo("TX-999000");

        Optional<Payment> byTxId = paymentRepository.findByTransactionId("TX-999000");
        assertThat(byTxId).isPresent();
        assertThat(byTxId.get().getAmount()).isEqualByComparingTo("10.00");
    }
}
