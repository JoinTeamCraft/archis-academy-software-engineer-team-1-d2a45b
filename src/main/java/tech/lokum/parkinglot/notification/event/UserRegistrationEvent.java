package tech.lokum.parkinglot.notification.event;

import tech.lokum.parkinglot.entity.User;

/**
 * Event published when a user successfully registers or is created.
 */
public record UserRegistrationEvent(User user) {
}
