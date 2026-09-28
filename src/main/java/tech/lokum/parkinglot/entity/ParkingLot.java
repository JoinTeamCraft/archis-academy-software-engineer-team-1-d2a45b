package tech.lokum.parkinglot.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity representing a physical parking lot facility.
 */
@Entity
@Table(
    name = "parking_lots",
    indexes = {
        @Index(name = "idx_parking_lot_name", columnList = "name"),
        @Index(name = "idx_parking_lot_location", columnList = "location")
    }
)
public class ParkingLot extends BaseEntity {

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "location", nullable = false, length = 255)
    private String location;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @Column(name = "hourly_rate", precision = 10, scale = 2)
    private BigDecimal hourlyRate;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "parkingLot", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ParkingSpot> spots = new ArrayList<>();

    public ParkingLot() {
    }

    public ParkingLot(String name, String location, Integer capacity) {
        this(name, location, capacity, BigDecimal.valueOf(5.00));
    }

    public ParkingLot(String name, String location, Integer capacity, BigDecimal hourlyRate) {
        this.name = name;
        this.location = location;
        this.capacity = capacity;
        this.hourlyRate = hourlyRate;
        this.active = true;
    }

    public void addSpot(ParkingSpot spot) {
        spots.add(spot);
        spot.setParkingLot(this);
    }

    public void removeSpot(ParkingSpot spot) {
        spots.remove(spot);
        spot.setParkingLot(null);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public BigDecimal getHourlyRate() {
        return hourlyRate;
    }

    public void setHourlyRate(BigDecimal hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public List<ParkingSpot> getSpots() {
        return spots;
    }

    public void setSpots(List<ParkingSpot> spots) {
        this.spots = spots;
    }
}
