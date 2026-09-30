package tech.lokum.parkinglot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import tech.lokum.parkinglot.entity.Role;
import tech.lokum.parkinglot.validation.PasswordStrength;
import tech.lokum.parkinglot.validation.UniqueEmail;
import tech.lokum.parkinglot.validation.UniqueUsername;

/**
 * Request payload for registering / creating a new user account with comprehensive validation constraints.
 */
@Schema(description = "Request body for user registration with input validation")
public record RegisterRequest(
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @UniqueUsername(message = "Username must be unique")
    @Schema(description = "Unique username", example = "john_doe")
    String username,

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email format")
    @UniqueEmail(message = "Email must be unique")
    @Schema(description = "User email address", example = "john.doe@example.com")
    String email,

    @NotBlank(message = "Password is required")
    @PasswordStrength(message = "Password must contain at least 8 characters")
    @Schema(description = "Password (minimum 8 characters)", example = "SecurePassword123")
    String password,

    @Size(max = 100, message = "Full name cannot exceed 100 characters")
    @Schema(description = "User's full name", example = "John Doe")
    String fullName,

    @Size(max = 30, message = "Phone number cannot exceed 30 characters")
    @Schema(description = "User's phone number", example = "+1234567890")
    String phoneNumber,

    @Schema(description = "User role", example = "USER")
    Role role
) {
    public RegisterRequest(String username, String email, String password) {
        this(username, email, password, null, null, Role.USER);
    }
}
