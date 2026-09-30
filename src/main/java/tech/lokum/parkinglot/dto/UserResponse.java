package tech.lokum.parkinglot.dto;

import tech.lokum.parkinglot.entity.Role;
import tech.lokum.parkinglot.entity.User;

/**
 * DTO representing user account details.
 */
public record UserResponse(
    Long id,
    String email,
    String fullName,
    String phoneNumber,
    Role role,
    boolean active
) {
    public UserResponse(Long id, String email, String fullName, Role role) {
        this(id, email, fullName, null, role, true);
    }

    public static UserResponse fromEntity(User user) {
        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getFullName(),
            user.getPhoneNumber(),
            user.getRole(),
            user.isActive()
        );
    }
}