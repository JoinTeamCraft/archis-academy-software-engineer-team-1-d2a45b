package tech.lokum.parkinglot.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import tech.lokum.parkinglot.dto.CreateReservationRequest;
import tech.lokum.parkinglot.dto.ReservationResponse;
import tech.lokum.parkinglot.dto.ReservationStatusResponse;
import tech.lokum.parkinglot.dto.UpdateReservationRequest;
import tech.lokum.parkinglot.entity.ParkingLot;
import tech.lokum.parkinglot.entity.ParkingSpot;
import tech.lokum.parkinglot.entity.Reservation;
import tech.lokum.parkinglot.entity.ReservationStatus;
import tech.lokum.parkinglot.entity.Role;
import tech.lokum.parkinglot.entity.SpotStatus;
import tech.lokum.parkinglot.entity.User;
import tech.lokum.parkinglot.entity.Vehicle;
import tech.lokum.parkinglot.entity.VehicleType;
import tech.lokum.parkinglot.exception.BadRequestException;
import tech.lokum.parkinglot.exception.ConflictException;
import tech.lokum.parkinglot.exception.ForbiddenException;
import tech.lokum.parkinglot.exception.ResourceNotFoundException;
import tech.lokum.parkinglot.repository.ParkingSpotRepository;
import tech.lokum.parkinglot.repository.ReservationRepository;
import tech.lokum.parkinglot.repository.UserRepository;
import tech.lokum.parkinglot.repository.VehicleRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private ParkingSpotRepository parkingSpotRepository;

    @InjectMocks
    private ReservationService reservationService;

    private User user;
    private Vehicle car;
    private Vehicle motorbike;
    private ParkingLot lot;
    private ParkingSpot spot;
    private Reservation reservation;

    @BeforeEach
    void setUp() {
        user = new User("alice@example.com", "secretpass", "Alice Smith", "123-456", Role.CUSTOMER);
        user.setId(10L);

        car = new Vehicle("ABC-1234", VehicleType.CAR, "Honda", "Civic", "Silver", user);
        car.setId(20L);

        motorbike = new Vehicle("MOTO-99", VehicleType.MOTORBIKE, "Yamaha", "R1", "Black", user);
        motorbike.setId(21L);

        lot = new ParkingLot("Central Garage", "Downtown", 100);
        lot.setId(1L);
        lot.setHourlyRate(BigDecimal.valueOf(10.00));

        spot = new ParkingSpot("A-01", 1, VehicleType.CAR, SpotStatus.AVAILABLE, BigDecimal.valueOf(12.00), lot);
        spot.setId(30L);

        Instant start = Instant.now().plus(2, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);
        Instant end = start.plus(2, ChronoUnit.HOURS);
        reservation = new Reservation(user, car, spot, start, end, BigDecimal.valueOf(24.00), ReservationStatus.CONFIRMED);
        reservation.setId(100L);
    }

    @Test
    @DisplayName("createReservation should successfully create and return reservation response")
    void shouldCreateReservationSuccessfully() {
        Instant start = Instant.now().plus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);
        Instant end = start.plus(2, ChronoUnit.HOURS);

        CreateReservationRequest request = new CreateReservationRequest(
            user.getId(), car.getId(), spot.getId(), start, end
        );

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(vehicleRepository.findById(car.getId())).thenReturn(Optional.of(car));
        when(parkingSpotRepository.findById(spot.getId())).thenReturn(Optional.of(spot));
        when(reservationRepository.hasOverlappingReservations(eq(spot.getId()), eq(start), eq(end), any()))
            .thenReturn(false);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
            Reservation r = invocation.getArgument(0);
            r.setId(101L);
            return r;
        });

        ReservationResponse response = reservationService.createReservation(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(101L);
        assertThat(response.userId()).isEqualTo(user.getId());
        assertThat(response.vehicleLicensePlate()).isEqualTo("ABC-1234");
        assertThat(response.spotNumber()).isEqualTo("A-01");
        assertThat(response.totalAmount()).isEqualByComparingTo("24.00"); // 2 hours * $12.00
        assertThat(response.status()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    @DisplayName("createReservation should throw ConflictException when spot has overlapping bookings")
    void shouldThrowConflictWhenOverlappingReservationExists() {
        Instant start = Instant.now().plus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);
        Instant end = start.plus(2, ChronoUnit.HOURS);

        CreateReservationRequest request = new CreateReservationRequest(
            user.getId(), car.getId(), spot.getId(), start, end
        );

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(vehicleRepository.findById(car.getId())).thenReturn(Optional.of(car));
        when(parkingSpotRepository.findById(spot.getId())).thenReturn(Optional.of(spot));
        when(reservationRepository.hasOverlappingReservations(eq(spot.getId()), eq(start), eq(end), any()))
            .thenReturn(true);

        assertThatThrownBy(() -> reservationService.createReservation(request))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("already reserved");
    }

    @Test
    @DisplayName("createReservation should throw BadRequestException when vehicle type does not match spot type")
    void shouldThrowBadRequestWhenVehicleTypeIncompatible() {
        Instant start = Instant.now().plus(1, ChronoUnit.HOURS);
        Instant end = start.plus(2, ChronoUnit.HOURS);

        // spot is CAR, but vehicle is MOTORBIKE
        CreateReservationRequest request = new CreateReservationRequest(
            user.getId(), motorbike.getId(), spot.getId(), start, end
        );

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(vehicleRepository.findById(motorbike.getId())).thenReturn(Optional.of(motorbike));
        when(parkingSpotRepository.findById(spot.getId())).thenReturn(Optional.of(spot));

        assertThatThrownBy(() -> reservationService.createReservation(request))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("cannot accommodate vehicle type");
    }

    @Test
    @DisplayName("createReservation should throw BadRequestException when vehicle does not belong to user")
    void shouldThrowBadRequestWhenVehicleBelongsToDifferentUser() {
        User otherUser = new User("other@example.com", "pass", "Bob", "555", Role.CUSTOMER);
        otherUser.setId(99L);
        Vehicle otherCar = new Vehicle("XYZ-9999", VehicleType.CAR, "Ford", "Focus", "Red", otherUser);
        otherCar.setId(99L);

        Instant start = Instant.now().plus(1, ChronoUnit.HOURS);
        Instant end = start.plus(2, ChronoUnit.HOURS);

        CreateReservationRequest request = new CreateReservationRequest(
            user.getId(), otherCar.getId(), spot.getId(), start, end
        );

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(vehicleRepository.findById(otherCar.getId())).thenReturn(Optional.of(otherCar));

        assertThatThrownBy(() -> reservationService.createReservation(request))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("does not belong to user");
    }

    @Test
    @DisplayName("createReservation should throw BadRequestException when end time is before start time")
    void shouldThrowBadRequestWhenEndTimeIsBeforeStartTime() {
        Instant start = Instant.now().plus(2, ChronoUnit.HOURS);
        Instant end = start.minus(1, ChronoUnit.HOURS); // invalid!

        CreateReservationRequest request = new CreateReservationRequest(
            user.getId(), car.getId(), spot.getId(), start, end
        );

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(vehicleRepository.findById(car.getId())).thenReturn(Optional.of(car));
        when(parkingSpotRepository.findById(spot.getId())).thenReturn(Optional.of(spot));

        assertThatThrownBy(() -> reservationService.createReservation(request))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("end time must be strictly after start time");
    }

    @Test
    @DisplayName("createReservation should throw ConflictException when spot is under maintenance")
    void shouldThrowConflictWhenSpotIsNotAvailable() {
        spot.setStatus(SpotStatus.MAINTENANCE);
        Instant start = Instant.now().plus(1, ChronoUnit.HOURS);
        Instant end = start.plus(2, ChronoUnit.HOURS);

        CreateReservationRequest request = new CreateReservationRequest(
            user.getId(), car.getId(), spot.getId(), start, end
        );

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(vehicleRepository.findById(car.getId())).thenReturn(Optional.of(car));
        when(parkingSpotRepository.findById(spot.getId())).thenReturn(Optional.of(spot));

        assertThatThrownBy(() -> reservationService.createReservation(request))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("currently not available");
    }

    @Test
    @DisplayName("updateReservation should reschedule time window and recompute price")
    void shouldUpdateReservationSuccessfully() {
        Instant newStart = reservation.getStartTime().plus(1, ChronoUnit.HOURS);
        Instant newEnd = reservation.getEndTime().plus(2, ChronoUnit.HOURS); // 3 hours total

        UpdateReservationRequest request = new UpdateReservationRequest(newStart, newEnd, null, null, null);

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.hasOverlappingReservationsExcludingId(
            eq(spot.getId()), eq(100L), eq(newStart), eq(newEnd), any()
        )).thenReturn(false);
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReservationResponse response = reservationService.updateReservation(100L, request);

        assertThat(response.startTime()).isEqualTo(newStart);
        assertThat(response.endTime()).isEqualTo(newEnd);
        assertThat(response.totalAmount()).isEqualByComparingTo("36.00"); // 3h * $12
    }

    @Test
    @DisplayName("updateReservation should throw ConflictException when rescheduled time overlaps")
    void shouldThrowConflictWhenUpdatedTimeOverlaps() {
        Instant newStart = reservation.getStartTime().plus(1, ChronoUnit.HOURS);
        Instant newEnd = reservation.getEndTime().plus(2, ChronoUnit.HOURS);

        UpdateReservationRequest request = new UpdateReservationRequest(newStart, newEnd, null, null, null);

        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.hasOverlappingReservationsExcludingId(
            eq(spot.getId()), eq(100L), eq(newStart), eq(newEnd), any()
        )).thenReturn(true);

        assertThatThrownBy(() -> reservationService.updateReservation(100L, request))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("already booked for the updated time window");
    }

    @Test
    @DisplayName("cancelReservation should transition status to CANCELLED")
    void shouldCancelReservationSuccessfully() {
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReservationResponse response = reservationService.cancelReservation(100L);

        assertThat(response.status()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    @DisplayName("cancelReservation should throw BadRequestException if reservation is already completed")
    void shouldThrowBadRequestWhenCancellingCompletedReservation() {
        reservation.setStatus(ReservationStatus.COMPLETED);
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.cancelReservation(100L))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("Cannot cancel an already completed");
    }

    @Test
    @DisplayName("deleteReservation should delete by ID when exists")
    void shouldDeleteReservationSuccessfully() {
        when(reservationRepository.existsById(100L)).thenReturn(true);

        reservationService.deleteReservation(100L);

        verify(reservationRepository).deleteById(100L);
    }

    @Test
    @DisplayName("deleteReservation should throw ResourceNotFoundException when not found")
    void shouldThrowNotFoundWhenDeletingNonExistentReservation() {
        when(reservationRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> reservationService.deleteReservation(999L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getReservationById should return reservation response when found")
    void shouldGetReservationById() {
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

        ReservationResponse response = reservationService.getReservationById(100L);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.userEmail()).isEqualTo("alice@example.com");
    }

    @Test
    @DisplayName("getReservationByIdAndUserId should throw ForbiddenException when user is not owner")
    void shouldThrowForbiddenWhenUserIsNotOwner() {
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.getReservationByIdAndUserId(100L, 999L))
            .isInstanceOf(ForbiddenException.class)
            .hasMessageContaining("not authorized");
    }

    @Test
    @DisplayName("getAllReservations with pageable should return paginated DTOs")
    void shouldGetAllReservationsPaginated() {
        Pageable pageable = PageRequest.of(0, 10);
        when(reservationRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(reservation), pageable, 1));

        Page<ReservationResponse> page = reservationService.getAllReservations(pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).id()).isEqualTo(100L);
    }

    @Test
    @DisplayName("calculateReservationPrice should compute ceil hours multiplied by spot rate")
    void shouldCalculatePriceCorrectly() {
        Instant start = Instant.parse("2026-10-01T10:00:00Z");
        Instant end = Instant.parse("2026-10-01T11:15:00Z"); // 75 mins -> 2 billable hours

        BigDecimal price = reservationService.calculateReservationPrice(spot, start, end);

        // 2 hours * $12.00 = $24.00
        assertThat(price).isEqualByComparingTo("24.00");
    }

    @Test
    @DisplayName("updateReservationStatus should successfully transition status to CANCELLED")
    void shouldUpdateReservationStatusToCancelled() {
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.updateStatusById(100L, ReservationStatus.CANCELLED)).thenReturn(1);

        ReservationStatusResponse response = reservationService.updateReservationStatus(100L, ReservationStatus.CANCELLED);

        assertThat(response).isNotNull();
        assertThat(response.reservationId()).isEqualTo(100L);
        assertThat(response.status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        verify(reservationRepository).updateStatusById(100L, ReservationStatus.CANCELLED);
    }

    @Test
    @DisplayName("updateReservationStatus should return immediately if already in target status")
    void shouldBeIdempotentWhenStatusAlreadyMatches() {
        reservation.setStatus(ReservationStatus.CONFIRMED);
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

        ReservationStatusResponse response = reservationService.updateReservationStatus(100L, ReservationStatus.CONFIRMED);

        assertThat(response.reservationId()).isEqualTo(100L);
        assertThat(response.status()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    @DisplayName("updateReservationStatus should successfully re-confirm a cancelled reservation when no conflict")
    void shouldReconfirmCancelledReservationWhenNoOverlap() {
        reservation.setStatus(ReservationStatus.CANCELLED);
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.hasOverlappingReservationsExcludingId(
            eq(spot.getId()), eq(100L), eq(reservation.getStartTime()), eq(reservation.getEndTime()), any()
        )).thenReturn(false);
        when(reservationRepository.updateStatusById(100L, ReservationStatus.CONFIRMED)).thenReturn(1);

        ReservationStatusResponse response = reservationService.updateReservationStatus(100L, ReservationStatus.CONFIRMED);

        assertThat(response.reservationId()).isEqualTo(100L);
        assertThat(response.status()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    @DisplayName("updateReservationStatus should throw ConflictException when re-confirming but spot is double-booked")
    void shouldThrowConflictWhenReconfirmingSpotWithOverlap() {
        reservation.setStatus(ReservationStatus.CANCELLED);
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.hasOverlappingReservationsExcludingId(
            eq(spot.getId()), eq(100L), eq(reservation.getStartTime()), eq(reservation.getEndTime()), any()
        )).thenReturn(true);

        assertThatThrownBy(() -> reservationService.updateReservationStatus(100L, ReservationStatus.CONFIRMED))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("already reserved by another booking");
    }

    @Test
    @DisplayName("updateReservationStatus should throw BadRequestException if reservation is already COMPLETED")
    void shouldThrowBadRequestWhenUpdatingCompletedReservation() {
        reservation.setStatus(ReservationStatus.COMPLETED);
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.updateReservationStatus(100L, ReservationStatus.CANCELLED))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("Cannot change status of a COMPLETED reservation");
    }

    @Test
    @DisplayName("updateReservationStatus should throw BadRequestException if reservation is EXPIRED")
    void shouldThrowBadRequestWhenUpdatingExpiredReservation() {
        reservation.setStatus(ReservationStatus.EXPIRED);
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.updateReservationStatus(100L, ReservationStatus.CANCELLED))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("Cannot change status of an EXPIRED reservation");
    }

    @Test
    @DisplayName("updateReservationStatus should throw BadRequestException if CANCELLED and target is not CONFIRMED")
    void shouldThrowBadRequestWhenUpdatingCancelledToNonConfirmed() {
        reservation.setStatus(ReservationStatus.CANCELLED);
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.updateReservationStatus(100L, ReservationStatus.ACTIVE))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("Cannot modify a CANCELLED reservation unless re-confirming");
    }

    @Test
    @DisplayName("updateReservationStatus should throw ResourceNotFoundException when reservation does not exist")
    void shouldThrowNotFoundWhenReservationDoesNotExist() {
        when(reservationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.updateReservationStatus(999L, ReservationStatus.CANCELLED))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Reservation not found with id: '999'");
    }

    @Test
    @DisplayName("updateReservationStatus should throw BadRequestException when status is null")
    void shouldThrowBadRequestWhenStatusIsNull() {
        assertThatThrownBy(() -> reservationService.updateReservationStatus(100L, null))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("Status is required");
    }
}
