package tech.lokum.parkinglot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.lokum.parkinglot.dto.PageResponse;
import tech.lokum.parkinglot.dto.RegisterRequest;
import tech.lokum.parkinglot.dto.UserResponse;
import tech.lokum.parkinglot.entity.Role;
import tech.lokum.parkinglot.entity.User;
import tech.lokum.parkinglot.exception.BadRequestException;
import tech.lokum.parkinglot.exception.ConflictException;
import tech.lokum.parkinglot.exception.ResourceNotFoundException;
import tech.lokum.parkinglot.exception.ValidationException;
import tech.lokum.parkinglot.notification.event.UserRegistrationEvent;
import tech.lokum.parkinglot.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing user entities, roles, and administrative operations.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            ObjectProvider<ApplicationEventPublisher> eventPublisherProvider
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisherProvider != null ? eventPublisherProvider.getIfAvailable() : null;
    }

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this(userRepository, passwordEncoder, null);
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
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new UserRegistrationEvent(saved));
        }
        return UserResponse.fromEntity(saved);
    }

    /**
     * Registers a new user with custom validation logic for unique username and password strength.
     */
    @Transactional
    public UserResponse registerUser(RegisterRequest request) {
        if (request == null) {
            throw new BadRequestException("Registration payload is required");
        }

        List<String> validationDetails = new ArrayList<>();

        String username = request.username() != null ? request.username().trim() : null;
        String email = request.email() != null ? request.email().trim().toLowerCase() : null;
        String password = request.password();

        if (username == null || username.isBlank()) {
            validationDetails.add("Username is required");
        } else if (userRepository.existsByUsernameIgnoreCase(username) || userRepository.existsByEmailIgnoreCase(username)) {
            validationDetails.add("Username must be unique");
        }

        if (email == null || email.isBlank()) {
            validationDetails.add("Email is required");
        } else if (userRepository.existsByEmailIgnoreCase(email)) {
            validationDetails.add("Email must be unique");
        }

        if (password == null || password.isBlank()) {
            validationDetails.add("Password is required");
        } else if (password.length() < 8) {
            validationDetails.add("Password must contain at least 8 characters");
        }

        if (!validationDetails.isEmpty()) {
            throw new ValidationException(validationDetails);
        }

        Role assignedRole = request.role() != null ? request.role() : Role.USER;
        User user = new User(
                username,
                email,
                passwordEncoder.encode(password),
                request.fullName() != null ? request.fullName().trim() : "",
                request.phoneNumber() != null ? request.phoneNumber().trim() : null,
                assignedRole
        );

        User saved = userRepository.save(user);
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new UserRegistrationEvent(saved));
        }
        return UserResponse.fromEntity(saved);
    }
}
