package tech.lokum.parkinglot.notification.event;

import tech.lokum.parkinglot.entity.Reservation;

/**
 * Event published when a parking reservation is successfully created/confirmed.
 */
public record BookingConfirmationEvent(Reservation reservation) {
}
