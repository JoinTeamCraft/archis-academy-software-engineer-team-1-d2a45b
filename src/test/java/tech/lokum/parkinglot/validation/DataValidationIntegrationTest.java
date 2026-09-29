package tech.lokum.parkinglot.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tech.lokum.parkinglot.dto.RegisterRequest;
import tech.lokum.parkinglot.entity.Role;
import tech.lokum.parkinglot.entity.User;
import tech.lokum.parkinglot.repository.UserRepository;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DataValidationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        User existingUser = new User(
                "existing_user",
                "existing@example.com",
                "$2a$10$abcdefghijklmnopqrstuvwxyz1234567890",
                "Existing User",
                null,
                Role.USER
        );
        userRepository.save(existingUser);
    }

    @Test
    @DisplayName("Should return 400 with validation details when username is not unique and password is too short")
    void shouldReturnValidationFailedWithExactDetails() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "existing_user",
                "unique.email@example.com",
                "short",
                "Test User",
                "+1234567890",
                Role.USER
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Validation failed")))
                .andExpect(jsonPath("$.details", hasItem("Username must be unique")))
                .andExpect(jsonPath("$.details", hasItem("Password must contain at least 8 characters")));
    }

    @Test
    @DisplayName("Should return 400 when standard validation annotations fail (@Email, @NotBlank)")
    void shouldReturnValidationErrorsForStandardAnnotations() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "",
                "invalid-email-format",
                "",
                "Test User",
                "+1234567890",
                Role.USER
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Validation failed")))
                .andExpect(jsonPath("$.details", hasItem("Username is required")))
                .andExpect(jsonPath("$.details", hasItem("Must be a valid email format")))
                .andExpect(jsonPath("$.details", hasItem("Password is required")));
    }

    @Test
    @DisplayName("Should return 400 when email is not unique")
    void shouldReturnValidationErrorForDuplicateEmail() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "brand_new_user",
                "existing@example.com",
                "ValidPassword123",
                "Test User",
                null,
                Role.USER
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Validation failed")))
                .andExpect(jsonPath("$.details", hasItem("Email must be unique")));
    }

    @Test
    @DisplayName("Should succeed with 201 Created when all inputs are valid")
    void shouldSucceedWhenAllInputsAreValid() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "john_doe",
                "john.doe@example.com",
                "ValidSecurePassword123!",
                "John Doe",
                "+1234567890",
                Role.USER
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email", is("john.doe@example.com")))
                .andExpect(jsonPath("$.role", is("USER")));
    }
}
