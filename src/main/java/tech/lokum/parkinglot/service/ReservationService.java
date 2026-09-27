package tech.lokum.parkinglot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.lokum.parkinglot.dto.CreateReservationRequest;
import tech.lokum.parkinglot.dto.ReservationResponse;
import tech.lokum.parkinglot.dto.ReservationStatusResponse;
import tech.lokum.parkinglot.dto.UpdateReservationRequest;
import tech.lokum.parkinglot.entity.ParkingSpot;
import tech.lokum.parkinglot.entity.Reservation;
import tech.lokum.parkinglot.entity.ReservationStatus;
import tech.lokum.parkinglot.entity.SpotStatus;
import tech.lokum.parkinglot.entity.User;
import tech.lokum.parkinglot.entity.Vehicle;
import tech.lokum.parkinglot.exception.BadRequestException;
import tech.lokum.parkinglot.exception.ConflictException;
import tech.lokum.parkinglot.exception.ForbiddenException;
import tech.lokum.parkinglot.exception.ResourceNotFoundException;
import tech.lokum.parkinglot.repository.ParkingSpotRepository;
import tech.lokum.parkinglot.repository.ReservationRepository;
import tech.lokum.parkinglot.repository.UserRepository;
import tech.lokum.parkinglot.repository.VehicleRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Service managing parking spot reservations, lifecycle transitions, double-booking prevention,
 * and duration-based pricing calculations.
 */
@Service
public class ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);

    private static final Set<ReservationStatus> INACTIVE_STATUSES = Set.of(
        ReservationStatus.CANCELLED,
        ReservationStatus.EXPIRED
    );

    private static final BigDecimal DEFAULT_HOURLY_RATE = BigDecimal.valueOf(10.00);

    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final VehicleRepository vehicleRepository;
    private final ParkingSpotRepository parkingSpotRepository;

    public ReservationService(
        ReservationRepository reservationRepository,
        UserRepository userRepository,
        VehicleRepository vehicleRepository,
        ParkingSpotRepository parkingSpotRepository
    ) {
        this.reservationRepository = reservationRepository;
        this.userRepository = userRepository;
        this.vehicleRepository = vehicleRepository;
        this.parkingSpotRepository = parkingSpotRepository;
    }

    /**
     * Creates a new reservation for a parking spot.
     * Enforces vehicle-to-spot type compatibility, ownership, time constraints, and double-booking prevention.
     *
     * @param request creation request payload
     * @return created reservation details
     */
    @Transactional
    public ReservationResponse createReservation(CreateReservationRequest request) {
        log.info("Creating reservation for vehicle {} on spot {}",
            request.vehicleId(), request.parkingSpotId());

        Vehicle vehicle = vehicleRepository.findById(request.vehicleId())
            .orElseThrow(() -> new ResourceNotFoundException("Vehicle", "id", request.vehicleId()));

        User user;
        if (request.userId() != null) {
            user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.userId()));
            validateVehicleOwnership(user, vehicle);
        } else {
            user = vehicle.getUser();
            if (user == null) {
                throw new BadRequestException(String.format("Vehicle '%s' is not associated with any registered user", vehicle.getLicensePlate()));
            }
        }

        ParkingSpot spot = parkingSpotRepository.findById(request.parkingSpotId())
            .orElseThrow(() -> new ResourceNotFoundException("ParkingSpot", "id", request.parkingSpotId()));

        validateVehicleSpotCompatibility(spot, vehicle);
        validateReservationTimeWindow(request.startTime(), request.endTime());

        if (spot.getStatus() != SpotStatus.AVAILABLE) {
            throw new ConflictException(String.format(
                "Parking spot '%s' is currently not available for reservation (status: %s)",
                spot.getSpotNumber(), spot.getStatus()
            ));
        }

        if (reservationRepository.hasOverlappingReservations(spot.getId(), request.startTime(), request.endTime(), INACTIVE_STATUSES)) {
            throw new ConflictException(String.format(
                "Parking spot '%s' is already reserved for the requested time window",
                spot.getSpotNumber()
            ));
        }

        BigDecimal totalAmount = calculateReservationPrice(spot, request.startTime(), request.endTime());

        Reservation reservation = new Reservation(
            user,
            vehicle,
            spot,
            request.startTime(),
            request.endTime(),
            totalAmount,
            ReservationStatus.CONFIRMED
        );

        Reservation saved = reservationRepository.save(reservation);
        log.info("Successfully created reservation ID {} with total amount {}", saved.getId(), saved.getTotalAmount());
        return ReservationResponse.fromEntity(saved);
    }

    /**
     * Updates an existing reservation (time interval, spot, vehicle, or status).
     *
     * @param id reservation ID
     * @param request update payload
     * @return updated reservation details
     */
    @Transactional
    public ReservationResponse updateReservation(Long id, UpdateReservationRequest request) {
        log.info("Updating reservation ID {}", id);

        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Reservation", "id", id));

        if (reservation.getStatus() == ReservationStatus.CANCELLED ||
            reservation.getStatus() == ReservationStatus.COMPLETED ||
            reservation.getStatus() == ReservationStatus.EXPIRED) {
            throw new BadRequestException(String.format(
                "Cannot modify a reservation that is already %s", reservation.getStatus()
            ));
        }

        ParkingSpot spot = reservation.getParkingSpot();
        if (request.parkingSpotId() != null && !request.parkingSpotId().equals(spot.getId())) {
            spot = parkingSpotRepository.findById(request.parkingSpotId())
                .orElseThrow(() -> new ResourceNotFoundException("ParkingSpot", "id", request.parkingSpotId()));

            if (spot.getStatus() != SpotStatus.AVAILABLE) {
                throw new ConflictException(String.format("Target parking spot '%s' is not available", spot.getSpotNumber()));
            }
            reservation.setParkingSpot(spot);
        }

        Vehicle vehicle = reservation.getVehicle();
        if (request.vehicleId() != null && !request.vehicleId().equals(vehicle.getId())) {
            vehicle = vehicleRepository.findById(request.vehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", "id", request.vehicleId()));

            validateVehicleOwnership(reservation.getUser(), vehicle);
            reservation.setVehicle(vehicle);
        }

        validateVehicleSpotCompatibility(spot, vehicle);

        Instant newStart = request.startTime() != null ? request.startTime() : reservation.getStartTime();
        Instant newEnd = request.endTime() != null ? request.endTime() : reservation.getEndTime();

        boolean timeChanged = !newStart.equals(reservation.getStartTime()) ||
                              !newEnd.equals(reservation.getEndTime()) ||
                              request.parkingSpotId() != null;

        if (timeChanged) {
            validateReservationTimeWindow(newStart, newEnd);

            if (reservationRepository.hasOverlappingReservationsExcludingId(spot.getId(), id, newStart, newEnd, INACTIVE_STATUSES)) {
                throw new ConflictException(String.format(
                    "Parking spot '%s' is already booked for the updated time window",
                    spot.getSpotNumber()
                ));
            }

            reservation.setStartTime(newStart);
            reservation.setEndTime(newEnd);
            reservation.setTotalAmount(calculateReservationPrice(spot, newStart, newEnd));
        }

        if (request.status() != null) {
            reservation.setStatus(request.status());
        }

        Reservation updated = reservationRepository.save(reservation);
        log.info("Successfully updated reservation ID {}", updated.getId());
        return ReservationResponse.fromEntity(updated);
    }

    /**
     * Soft-deletes / cancels a reservation, immediately releasing the spot for new bookings.
     *
     * @param id reservation ID
     * @return cancelled reservation details
     */
    @Transactional
    public ReservationResponse cancelReservation(Long id) {
        log.info("Cancelling reservation ID {}", id);

        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Reservation", "id", id));

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            return ReservationResponse.fromEntity(reservation);
        }

        if (reservation.getStatus() == ReservationStatus.COMPLETED) {
            throw new BadRequestException("Cannot cancel an already completed parking reservation");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        Reservation saved = reservationRepository.save(reservation);
        log.info("Reservation ID {} has been cancelled", id);
        return ReservationResponse.fromEntity(saved);
    }

    /**
     * Updates the lifecycle status of an existing reservation (e.g., cancel or confirm).
     *
     * @param reservationId ID of the reservation to update
     * @param newStatus target reservation status
     * @return status update response DTO
     */
    @Transactional
    public ReservationStatusResponse updateReservationStatus(Long reservationId, ReservationStatus newStatus) {
        log.info("Updating reservation ID {} status to {}", reservationId, newStatus);

        if (reservationId == null) {
            throw new BadRequestException("Reservation ID is required");
        }
        if (newStatus == null) {
            throw new BadRequestException("Status is required");
        }

        Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new ResourceNotFoundException("Reservation", "id", reservationId));

        if (reservation.getStatus() == newStatus) {
            return ReservationStatusResponse.of(reservationId, newStatus);
        }

        if (reservation.getStatus() == ReservationStatus.COMPLETED) {
            throw new BadRequestException("Cannot change status of a COMPLETED reservation");
        }

        if (reservation.getStatus() == ReservationStatus.EXPIRED) {
            throw new BadRequestException("Cannot change status of an EXPIRED reservation");
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED && newStatus != ReservationStatus.CONFIRMED) {
            throw new BadRequestException("Cannot modify a CANCELLED reservation unless re-confirming");
        }

        if (newStatus == ReservationStatus.CONFIRMED || newStatus == ReservationStatus.ACTIVE) {
            if (reservationRepository.hasOverlappingReservationsExcludingId(
                reservation.getParkingSpot().getId(),
                reservationId,
                reservation.getStartTime(),
                reservation.getEndTime(),
                INACTIVE_STATUSES
            )) {
                throw new ConflictException(String.format(
                    "Cannot set reservation to %s: Parking spot '%s' is already reserved by another booking for this time window",
                    newStatus, reservation.getParkingSpot().getSpotNumber()
                ));
            }
        }

        reservation.setStatus(newStatus);
        reservationRepository.updateStatusById(reservationId, newStatus);
        log.info("Successfully updated reservation ID {} status to {}", reservationId, newStatus);
        return ReservationStatusResponse.of(reservationId, newStatus);
    }

    /**
     * Hard deletes a reservation from persistence.
     *
     * @param id reservation ID
     */
    @Transactional
    public void deleteReservation(Long id) {
        log.info("Deleting reservation ID {}", id);
        if (!reservationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Reservation", "id", id);
        }
        reservationRepository.deleteById(id);
    }

    /**
     * Retrieves reservation details by ID.
     *
     * @param id reservation ID
     * @return reservation details
     */
    @Transactional(readOnly = true)
    public ReservationResponse getReservationById(Long id) {
        log.debug("Fetching reservation ID {}", id);
        return reservationRepository.findById(id)
            .map(ReservationResponse::fromEntity)
            .orElseThrow(() -> new ResourceNotFoundException("Reservation", "id", id));
    }

    /**
     * Retrieves reservation details by ID and verifies user ownership.
     *
     * @param id reservation ID
     * @param userId user ID
     * @return reservation details
     */
    @Transactional(readOnly = true)
    public ReservationResponse getReservationByIdAndUserId(Long id, Long userId) {
        log.debug("Fetching reservation ID {} for user ID {}", id, userId);
        Reservation reservation = reservationRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Reservation", "id", id));

        if (reservation.getUser() == null || !reservation.getUser().getId().equals(userId)) {
            throw new ForbiddenException("You are not authorized to view this reservation");
        }

        return ReservationResponse.fromEntity(reservation);
    }

    /**
     * Retrieves all reservations as a list.
     */
    @Transactional(readOnly = true)
    public List<ReservationResponse> getAllReservations() {
        return reservationRepository.findAll()
            .stream()
            .map(ReservationResponse::fromEntity)
            .toList();
    }

    /**
     * Retrieves a paginated list of all reservations.
     */
    @Transactional(readOnly = true)
    public Page<ReservationResponse> getAllReservations(Pageable pageable) {
        return reservationRepository.findAll(pageable)
            .map(ReservationResponse::fromEntity);
    }

    /**
     * Retrieves reservations created by a specific user.
     */
    @Transactional(readOnly = true)
    public List<ReservationResponse> getReservationsByUserId(Long userId) {
        return reservationRepository.findByUserId(userId)
            .stream()
            .map(ReservationResponse::fromEntity)
            .toList();
    }

    /**
     * Retrieves a paginated list of reservations for a specific user.
     */
    @Transactional(readOnly = true)
    public Page<ReservationResponse> getReservationsByUserId(Long userId, Pageable pageable) {
        return reservationRepository.findByUserId(userId, pageable)
            .map(ReservationResponse::fromEntity);
    }

    /**
     * Retrieves all reservations for a specific spot.
     */
    @Transactional(readOnly = true)
    public List<ReservationResponse> getReservationsBySpotId(Long spotId) {
        return reservationRepository.findByParkingSpotId(spotId)
            .stream()
            .map(ReservationResponse::fromEntity)
            .toList();
    }

    /**
     * Retrieves a paginated list of reservations filtered by status.
     */
    @Transactional(readOnly = true)
    public Page<ReservationResponse> getReservationsByStatus(ReservationStatus status, Pageable pageable) {
        return reservationRepository.findByStatus(status, pageable)
            .map(ReservationResponse::fromEntity);
    }

    /**
     * Calculates the reservation parking charge based on billable duration and hourly rate.
     */
    public BigDecimal calculateReservationPrice(ParkingSpot spot, Instant startTime, Instant endTime) {
        BigDecimal hourlyRate = spot.getHourlyRate();
        if (hourlyRate == null || hourlyRate.compareTo(BigDecimal.ZERO) <= 0) {
            hourlyRate = DEFAULT_HOURLY_RATE;
        }

        long minutes = Math.max(1, Duration.between(startTime, endTime).toMinutes());
        long billableHours = Math.max(1, (long) Math.ceil((double) minutes / 60.0));

        return hourlyRate.multiply(BigDecimal.valueOf(billableHours)).setScale(2, RoundingMode.HALF_UP);
    }

    private void validateVehicleOwnership(User user, Vehicle vehicle) {
        if (vehicle.getUser() == null || !vehicle.getUser().getId().equals(user.getId())) {
            throw new BadRequestException(String.format(
                "Vehicle '%s' does not belong to user ID '%d'",
                vehicle.getLicensePlate(), user.getId()
            ));
        }
    }

    private void validateVehicleSpotCompatibility(ParkingSpot spot, Vehicle vehicle) {
        if (!spot.canAccommodate(vehicle.getVehicleType())) {
            throw new BadRequestException(String.format(
                "Spot '%s' (type %s) cannot accommodate vehicle type %s",
                spot.getSpotNumber(), spot.getSpotType(), vehicle.getVehicleType()
            ));
        }
    }

    private void validateReservationTimeWindow(Instant startTime, Instant endTime) {
        if (startTime == null || endTime == null) {
            throw new BadRequestException("Start time and end time are required");
        }
        if (!endTime.isAfter(startTime)) {
            throw new BadRequestException("Reservation end time must be strictly after start time");
        }
        // Allow a 5-minute clock drift margin for network delays or local clock differences
        Instant cutoff = Instant.now().minus(Duration.ofMinutes(5));
        if (startTime.isBefore(cutoff)) {
            throw new BadRequestException("Reservation start time cannot be in the past");
        }
    }
}
