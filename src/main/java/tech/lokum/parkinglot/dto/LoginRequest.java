package tech.lokum.parkinglot.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for user authentication.
 */
public record LoginRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email format")
    String email,

    @NotBlank(message = "Password is required")
    String password
) {}