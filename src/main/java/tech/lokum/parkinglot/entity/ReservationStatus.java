package tech.lokum.parkinglot.entity;

/**
 * Lifecycle states of a spot reservation.
 */
public enum ReservationStatus {
    PENDING,
    CONFIRMED,
    ACTIVE,
    COMPLETED,
    CANCELLED,
    EXPIRED
}
