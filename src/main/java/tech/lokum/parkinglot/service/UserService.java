package tech.lokum.parkinglot.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.lokum.parkinglot.dto.PageResponse;
import tech.lokum.parkinglot.dto.UserResponse;
import tech.lokum.parkinglot.entity.Role;
import tech.lokum.parkinglot.entity.User;
import tech.lokum.parkinglot.exception.BadRequestException;
import tech.lokum.parkinglot.exception.ConflictException;
import tech.lokum.parkinglot.exception.ResourceNotFoundException;
import tech.lokum.parkinglot.repository.UserRepository;

/**
 * Service for managing user entities, roles, and administrative operations.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Retrieves user by unique ID.
     */
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        if (id == null || id <= 0) {
            throw new BadRequestException("Valid user ID is required");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return UserResponse.fromEntity(user);
    }

    /**
     * Retrieves paginated list of users.
     */
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAllUsers(Pageable pageable) {
        Page<UserResponse> page = userRepository.findAll(pageable).map(UserResponse::fromEntity);
        return PageResponse.from(page);
    }

    /**
     * Deletes user by ID. Only accessible by ADMIN.
     */
    @Transactional
    public void deleteUser(Long id) {
        if (id == null || id <= 0) {
            throw new BadRequestException("Valid user ID is required");
        }
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        userRepository.delete(user);
    }

    /**
     * Creates / registers a new user with encoded password and assigned role.
     */
    @Transactional
    public UserResponse createUser(String email, String rawPassword, String fullName, String phone, Role role) {
        if (email == null || email.isBlank()) {
            throw new BadRequestException("Email is required");
        }
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new BadRequestException("Password is required");
        }
        if (userRepository.existsByEmailIgnoreCase(email.trim())) {
            throw new ConflictException("User already exists with email: " + email);
        }

        Role assignedRole = role != null ? role : Role.USER;
        User user = new User(
                email.trim().toLowerCase(),
                passwordEncoder.encode(rawPassword),
                fullName != null ? fullName.trim() : "",
                phone != null ? phone.trim() : null,
                assignedRole
        );

        User saved = userRepository.save(user);
        return UserResponse.fromEntity(saved);
    }
}
