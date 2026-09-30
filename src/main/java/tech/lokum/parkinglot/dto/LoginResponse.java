package tech.lokum.parkinglot.dto;

import tech.lokum.parkinglot.entity.Role;

/**
 * Response payload containing the issued JWT authentication token and user information.
 */
public record LoginResponse(
    String accessToken,
    String tokenType,
    Long userId,
    String email,
    Role role,
    long expiresIn
) {
    public LoginResponse(String accessToken, Long userId, String email, Role role, long expiresIn) {
        this(accessToken, "Bearer", userId, email, role, expiresIn);
    }
}