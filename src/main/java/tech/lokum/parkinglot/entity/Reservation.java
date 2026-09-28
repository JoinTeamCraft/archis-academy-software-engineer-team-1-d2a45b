package tech.lokum.parkinglot.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * JPA entity representing a parking spot reservation.
 */
@Entity
@Table(
    name = "reservations",
    indexes = {
        @Index(name = "idx_res_spot_time", columnList = "parking_spot_id, start_time, end_time"),
        @Index(name = "idx_res_user", columnList = "user_id"),
        @Index(name = "idx_res_status", columnList = "status")
    }
)
public class Reservation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parking_spot_id", nullable = false)
    private ParkingSpot parkingSpot;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "actual_entry_time")
    private Instant actualEntryTime;

    @Column(name = "actual_exit_time")
    private Instant actualExitTime;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReservationStatus status = ReservationStatus.PENDING;

    @OneToOne(mappedBy = "reservation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Payment payment;

    public Reservation() {
    }

    public Reservation(User user, Vehicle vehicle, ParkingSpot parkingSpot, Instant startTime, Instant endTime, BigDecimal totalAmount) {
        this(user, vehicle, parkingSpot, startTime, endTime, totalAmount, ReservationStatus.PENDING);
    }

    public Reservation(User user, Vehicle vehicle, ParkingSpot parkingSpot, Instant startTime, Instant endTime, BigDecimal totalAmount, ReservationStatus status) {
        this.user = user;
        this.vehicle = vehicle;
        this.parkingSpot = parkingSpot;
        this.startTime = startTime;
        this.endTime = endTime;
        this.totalAmount = totalAmount;
        this.status = status != null ? status : ReservationStatus.PENDING;
    }

    /**
     * Checks if this reservation's time window overlaps with the requested interval [start, end].
     * Adjacent time intervals (e.g. one ends exactly when the other starts) do not overlap.
     */
    public boolean overlapsWith(Instant start, Instant end) {
        return start.isBefore(this.endTime) && end.isAfter(this.startTime);
    }

    /**
     * Checks if this reservation is active or holding a spot.
     */
    public boolean isHoldingSpot() {
        return status == ReservationStatus.CONFIRMED || status == ReservationStatus.ACTIVE || status == ReservationStatus.PENDING;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public ParkingSpot getParkingSpot() {
        return parkingSpot;
    }

    public void setParkingSpot(ParkingSpot parkingSpot) {
        this.parkingSpot = parkingSpot;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public Instant getActualEntryTime() {
        return actualEntryTime;
    }

    public void setActualEntryTime(Instant actualEntryTime) {
        this.actualEntryTime = actualEntryTime;
    }

    public Instant getActualExitTime() {
        return actualExitTime;
    }

    public void setActualExitTime(Instant actualExitTime) {
        this.actualExitTime = actualExitTime;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
        if (payment != null && payment.getReservation() != this) {
            payment.setReservation(this);
        }
    }
}
