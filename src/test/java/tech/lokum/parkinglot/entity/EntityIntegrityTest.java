package tech.lokum.parkinglot.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class EntityIntegrityTest {

    @Test
    @DisplayName("User and Vehicle relationship should maintain bidirectional references")
    void testUserVehicleRelationship() {
        User user = new User("alice@example.com", "secret123", "Alice Smith", "1234567890", Role.CUSTOMER);
        Vehicle vehicle = new Vehicle("ABC-1234", VehicleType.CAR, "Toyota", "Corolla", "Silver", user);

        user.addVehicle(vehicle);

        assertThat(user.getVehicles()).contains(vehicle);
        assertThat(vehicle.getUser()).isEqualTo(user);

        user.removeVehicle(vehicle);
        assertThat(user.getVehicles()).doesNotContain(vehicle);
        assertThat(vehicle.getUser()).isNull();
    }

    @Test
    @DisplayName("ParkingLot and ParkingSpot relationship should maintain bidirectional references")
    void testParkingLotSpotRelationship() {
        ParkingLot lot = new ParkingLot("Central Deck", "100 Main St", 50, BigDecimal.valueOf(4.50));
        ParkingSpot spot = new ParkingSpot("S-101", 1, VehicleType.CAR, lot);

        lot.addSpot(spot);

        assertThat(lot.getSpots()).contains(spot);
        assertThat(spot.getParkingLot()).isEqualTo(lot);
        assertThat(spot.canAccommodate(VehicleType.CAR)).isTrue();
        assertThat(spot.canAccommodate(VehicleType.MOTORBIKE)).isFalse();

        lot.removeSpot(spot);
        assertThat(lot.getSpots()).doesNotContain(spot);
        assertThat(spot.getParkingLot()).isNull();
    }

    @Test
    @DisplayName("Reservation overlap logic should correctly identify overlapping and adjacent intervals")
    void testReservationOverlapLogic() {
        Instant now = Instant.now();
        Instant resStart = now.plus(2, ChronoUnit.HOURS);
        Instant resEnd = now.plus(4, ChronoUnit.HOURS);

        User user = new User("bob@example.com", "pass", "Bob", null, Role.CUSTOMER);
        Vehicle vehicle = new Vehicle("XYZ-9999", VehicleType.EV, "Tesla", "Model 3", "White", user);
        ParkingLot lot = new ParkingLot("EV Hub", "Station Rd", 20);
        ParkingSpot spot = new ParkingSpot("EV-1", 1, VehicleType.EV, lot);

        Reservation res = new Reservation(user, vehicle, spot, resStart, resEnd, BigDecimal.valueOf(15.00));

        // Completely overlapping window [resStart + 30m, resEnd - 30m]
        assertThat(res.overlapsWith(resStart.plus(30, ChronoUnit.MINUTES), resEnd.minus(30, ChronoUnit.MINUTES))).isTrue();

        // Overlapping at beginning [resStart - 1h, resStart + 1h]
        assertThat(res.overlapsWith(resStart.minus(1, ChronoUnit.HOURS), resStart.plus(1, ChronoUnit.HOURS))).isTrue();

        // Overlapping at end [resEnd - 1h, resEnd + 1h]
        assertThat(res.overlapsWith(resEnd.minus(1, ChronoUnit.HOURS), resEnd.plus(1, ChronoUnit.HOURS))).isTrue();

        // Adjacent before: ends exactly when res starts -> NO overlap
        assertThat(res.overlapsWith(resStart.minus(2, ChronoUnit.HOURS), resStart)).isFalse();

        // Adjacent after: starts exactly when res ends -> NO overlap
        assertThat(res.overlapsWith(resEnd, resEnd.plus(2, ChronoUnit.HOURS))).isFalse();

        // Completely outside before -> NO overlap
        assertThat(res.overlapsWith(resStart.minus(5, ChronoUnit.HOURS), resStart.minus(3, ChronoUnit.HOURS))).isFalse();

        // Completely outside after -> NO overlap
        assertThat(res.overlapsWith(resEnd.plus(1, ChronoUnit.HOURS), resEnd.plus(3, ChronoUnit.HOURS))).isFalse();
    }

    @Test
    @DisplayName("Payment should bind to Reservation and update status correctly")
    void testPaymentReservationRelationship() {
        User user = new User("carol@example.com", "pass", "Carol", null, Role.CUSTOMER);
        Vehicle vehicle = new Vehicle("M-4321", VehicleType.MOTORBIKE, "Honda", "CBR", "Black", user);
        ParkingLot lot = new ParkingLot("Bike Lot", "Bay St", 10);
        ParkingSpot spot = new ParkingSpot("B-1", 1, VehicleType.MOTORBIKE, lot);

        Instant start = Instant.now().plus(1, ChronoUnit.HOURS);
        Instant end = Instant.now().plus(3, ChronoUnit.HOURS);
        Reservation reservation = new Reservation(user, vehicle, spot, start, end, BigDecimal.valueOf(10.00));

        Payment payment = new Payment(reservation, BigDecimal.valueOf(10.00), PaymentMethod.SANDBOX);
        reservation.setPayment(payment);

        assertThat(reservation.getPayment()).isEqualTo(payment);
        assertThat(payment.getReservation()).isEqualTo(reservation);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);

        payment.markSuccess("TX-12345678");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getTransactionId()).isEqualTo("TX-12345678");
        assertThat(payment.getPaidAt()).isNotNull();
    }
}
