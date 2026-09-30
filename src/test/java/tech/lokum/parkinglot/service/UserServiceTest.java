package tech.lokum.parkinglot.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import tech.lokum.parkinglot.dto.PageResponse;
import tech.lokum.parkinglot.dto.UserResponse;
import tech.lokum.parkinglot.entity.Role;
import tech.lokum.parkinglot.entity.User;
import tech.lokum.parkinglot.exception.BadRequestException;
import tech.lokum.parkinglot.exception.ConflictException;
import tech.lokum.parkinglot.exception.ResourceNotFoundException;
import tech.lokum.parkinglot.repository.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User("test@example.com", "encodedPassword", "Test User", "+1234567890", Role.USER);
        sampleUser.setId(1L);
    }

    @Test
    @DisplayName("getUserById returns UserResponse when user exists")
    void testGetUserByIdSuccess() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        UserResponse response = userService.getUserById(1L);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("test@example.com", response.email());
        assertEquals(Role.USER, response.role());
    }

    @Test
    @DisplayName("getUserById throws BadRequestException for null or non-positive ID")
    void testGetUserByIdInvalid() {
        assertThrows(BadRequestException.class, () -> userService.getUserById(null));
        assertThrows(BadRequestException.class, () -> userService.getUserById(0L));
        assertThrows(BadRequestException.class, () -> userService.getUserById(-1L));
    }

    @Test
    @DisplayName("getUserById throws ResourceNotFoundException when user is not found")
    void testGetUserByIdNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(999L));
    }

    @Test
    @DisplayName("getAllUsers returns paginated PageResponse")
    void testGetAllUsers() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(List.of(sampleUser), pageable, 1);
        when(userRepository.findAll(pageable)).thenReturn(userPage);

        PageResponse<UserResponse> response = userService.getAllUsers(pageable);

        assertNotNull(response);
        assertEquals(1, response.totalElements());
        assertEquals("test@example.com", response.content().get(0).email());
    }

    @Test
    @DisplayName("deleteUser deletes user when valid ID provided")
    void testDeleteUserSuccess() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        userService.deleteUser(1L);

        verify(userRepository).delete(sampleUser);
    }

    @Test
    @DisplayName("deleteUser throws BadRequestException on invalid ID")
    void testDeleteUserInvalidId() {
        assertThrows(BadRequestException.class, () -> userService.deleteUser(-5L));
    }

    @Test
    @DisplayName("deleteUser throws ResourceNotFoundException when user not found")
    void testDeleteUserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.deleteUser(999L));
    }

    @Test
    @DisplayName("createUser saves and returns UserResponse with encoded password and role")
    void testCreateUserSuccess() {
        when(userRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("plainSecret")).thenReturn("encodedSecret");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(10L);
            return u;
        });

        UserResponse created = userService.createUser("new@example.com", "plainSecret", "New Admin", "+1000", Role.ADMIN);

        assertNotNull(created);
        assertEquals(10L, created.id());
        assertEquals("new@example.com", created.email());
        assertEquals(Role.ADMIN, created.role());
    }

    @Test
    @DisplayName("createUser throws ConflictException when email is already registered")
    void testCreateUserDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("test@example.com")).thenReturn(true);

        assertThrows(ConflictException.class, () ->
                userService.createUser("test@example.com", "pass", "Name", null, Role.USER));
    }

    @Test
    @DisplayName("createUser throws BadRequestException on missing email or password")
    void testCreateUserValidation() {
        assertThrows(BadRequestException.class, () ->
                userService.createUser("", "pass", "Name", null, Role.USER));
        assertThrows(BadRequestException.class, () ->
                userService.createUser("valid@example.com", "", "Name", null, Role.USER));
    }
}
