package tech.lokum.parkinglot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity representing an individual parking spot within a parking lot.
 */
@Entity
@Table(
    name = "parking_spots",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_spot_lot_number", columnNames = {"parking_lot_id", "spot_number"})
    },
    indexes = {
        @Index(name = "idx_spot_lot_status", columnList = "parking_lot_id, status"),
        @Index(name = "idx_spot_type", columnList = "spot_type")
    }
)
public class ParkingSpot extends BaseEntity {

    @Column(name = "spot_number", nullable = false, length = 20)
    private String spotNumber;

    @Column(name = "floor_number", nullable = false)
    private Integer floorNumber = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "spot_type", nullable = false, length = 20)
    private VehicleType spotType = VehicleType.CAR;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SpotStatus status = SpotStatus.AVAILABLE;

    @Column(name = "hourly_rate", precision = 10, scale = 2)
    private BigDecimal hourlyRate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parking_lot_id", nullable = false)
    private ParkingLot parkingLot;

    @OneToMany(mappedBy = "parkingSpot")
    private List<Reservation> reservations = new ArrayList<>();

    public ParkingSpot() {
    }

    public ParkingSpot(String spotNumber, Integer floorNumber, VehicleType spotType, ParkingLot parkingLot) {
        this(spotNumber, floorNumber, spotType, SpotStatus.AVAILABLE, null, parkingLot);
    }

    public ParkingSpot(String spotNumber, Integer floorNumber, VehicleType spotType, SpotStatus status, BigDecimal hourlyRate, ParkingLot parkingLot) {
        this.spotNumber = spotNumber;
        this.floorNumber = floorNumber != null ? floorNumber : 1;
        this.spotType = spotType != null ? spotType : VehicleType.CAR;
        this.status = status != null ? status : SpotStatus.AVAILABLE;
        this.hourlyRate = hourlyRate;
        this.parkingLot = parkingLot;
    }

    /**
     * Checks if this spot type can accommodate the given vehicle type.
     */
    public boolean canAccommodate(VehicleType vehicleType) {
        return this.spotType == vehicleType;
    }

    public String getSpotNumber() {
        return spotNumber;
    }

    public void setSpotNumber(String spotNumber) {
        this.spotNumber = spotNumber;
    }

    public Integer getFloorNumber() {
        return floorNumber;
    }

    public void setFloorNumber(Integer floorNumber) {
        this.floorNumber = floorNumber;
    }

    public VehicleType getSpotType() {
        return spotType;
    }

    public void setSpotType(VehicleType spotType) {
        this.spotType = spotType;
    }

    public SpotStatus getStatus() {
        return status;
    }

    public void setStatus(SpotStatus status) {
        this.status = status;
    }

    public BigDecimal getHourlyRate() {
        return hourlyRate != null ? hourlyRate : (parkingLot != null ? parkingLot.getHourlyRate() : null);
    }

    public void setHourlyRate(BigDecimal hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    public ParkingLot getParkingLot() {
        return parkingLot;
    }

    public void setParkingLot(ParkingLot parkingLot) {
        this.parkingLot = parkingLot;
    }

    public List<Reservation> getReservations() {
        return reservations;
    }

    public void setReservations(List<Reservation> reservations) {
        this.reservations = reservations;
    }
}
