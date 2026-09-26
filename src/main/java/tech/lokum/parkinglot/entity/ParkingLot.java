package tech.lokum.parkinglot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

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

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public ParkingLot() {
    }

    public ParkingLot(String name, String location, Integer capacity) {
        this.name = name;
        this.location = location;
        this.capacity = capacity;
        this.active = true;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
