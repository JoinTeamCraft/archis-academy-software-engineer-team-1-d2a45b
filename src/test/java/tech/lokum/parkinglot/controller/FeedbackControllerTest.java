package tech.lokum.parkinglot.controller;

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
import tech.lokum.parkinglot.entity.Feedback;
import tech.lokum.parkinglot.entity.Role;
import tech.lokum.parkinglot.entity.User;
import tech.lokum.parkinglot.repository.FeedbackRepository;
import tech.lokum.parkinglot.repository.UserRepository;
import tech.lokum.parkinglot.security.JwtService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests for POST /api/feedback through security, validation, the service and the database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FeedbackControllerTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FeedbackRepository feedbackRepository;

    private User alice;

    @BeforeEach
    void setUp() {
        alice = userRepository.save(new User("feedback-alice@test.com", "hashed", "Alice", null, Role.CUSTOMER));
        userRepository.save(new User("feedback-bob@test.com", "hashed", "Bob", null, Role.CUSTOMER));
    }

    private String tokenFor(String email, Role role) {
        return "Bearer " + jwtService.generateToken(email, role);
    }

    private static String body(Object userId, String feedbackText, Object rating) throws Exception {
        Map<String, Object> fields = new HashMap<>();
        fields.put("userId", userId);
        fields.put("feedbackText", feedbackText);
        fields.put("rating", rating);
        return objectMapper.writeValueAsString(fields);
    }

    @Test
    @DisplayName("POST /api/feedback should save the feedback and return 201 with id and submittedAt")
    void shouldSubmitFeedback() throws Exception {
        mockMvc.perform(post("/api/feedback")
                .header("Authorization", tokenFor(alice.getEmail(), Role.CUSTOMER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(alice.getId(), "Great service!", 5)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(jsonPath("$.userId").value(alice.getId()))
            .andExpect(jsonPath("$.feedbackText").value("Great service!"))
            .andExpect(jsonPath("$.rating").value(5))
            .andExpect(jsonPath("$.submittedAt").value(matchesPattern("\\d{4}-\\d{2}-\\d{2}T.*Z")));

        List<Feedback> saved = feedbackRepository.findAll();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getUser().getId()).isEqualTo(alice.getId());
        assertThat(saved.get(0).getRating()).isEqualTo(5);
    }

    @Test
    @DisplayName("POST /api/feedback should return 400 when the rating is outside 1 to 5")
    void shouldRejectOutOfRangeRating() throws Exception {
        mockMvc.perform(post("/api/feedback")
                .header("Authorization", tokenFor(alice.getEmail(), Role.CUSTOMER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(alice.getId(), "Great service!", 6)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.validationErrors.rating").value("Rating must be between 1 and 5"));

        assertThat(feedbackRepository.count()).isZero();
    }

    @Test
    @DisplayName("POST /api/feedback should return 400 when required fields are missing")
    void shouldRejectMissingFields() throws Exception {
        mockMvc.perform(post("/api/feedback")
                .header("Authorization", tokenFor(alice.getEmail(), Role.CUSTOMER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(null, null, null)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.details", hasItem("User ID is required")))
            .andExpect(jsonPath("$.details", hasItem("Feedback text is required")))
            .andExpect(jsonPath("$.details", hasItem("Rating is required")));
    }

    @Test
    @DisplayName("POST /api/feedback should return 400 for a malformed JSON body")
    void shouldRejectMalformedJson() throws Exception {
        mockMvc.perform(post("/api/feedback")
                .header("Authorization", tokenFor(alice.getEmail(), Role.CUSTOMER))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\": 1, \"rating\": \"five\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/feedback should return 401 when unauthenticated")
    void shouldRequireAuthentication() throws Exception {
        mockMvc.perform(post("/api/feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(alice.getId(), "Great service!", 5)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401));

        assertThat(feedbackRepository.count()).isZero();
    }

    @Test
    @DisplayName("POST /api/feedback should return 403 when submitting feedback as another user")
    void shouldForbidOtherUser() throws Exception {
        mockMvc.perform(post("/api/feedback")
                .header("Authorization", tokenFor("feedback-bob@test.com", Role.CUSTOMER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(alice.getId(), "Terrible service!", 1)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403));

        assertThat(feedbackRepository.count()).isZero();
    }

    @Test
    @DisplayName("POST /api/feedback should let an admin submit feedback on behalf of a user")
    void shouldAllowAdmin() throws Exception {
        mockMvc.perform(post("/api/feedback")
                .header("Authorization", tokenFor("admin@test.com", Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(alice.getId(), "Reported by phone", 3)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.userId").value(alice.getId()));
    }

    @Test
    @DisplayName("POST /api/feedback should return 404 when the user does not exist")
    void shouldReturnNotFoundForUnknownUser() throws Exception {
        mockMvc.perform(post("/api/feedback")
                .header("Authorization", tokenFor("admin@test.com", Role.ADMIN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(999_999, "Great service!", 5)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("POST /api/feedback should keep quotes, backslashes and newlines in the feedback text")
    void shouldKeepSpecialCharacters() throws Exception {
        String text = "Said \"great\" \\ then\nleft";

        mockMvc.perform(post("/api/feedback")
                .header("Authorization", tokenFor(alice.getEmail(), Role.CUSTOMER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(alice.getId(), text, 4)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.feedbackText").value(text));
    }

    @Test
    @DisplayName("POST /api/feedback should return 400 when the body is empty")
    void shouldRejectEmptyBody() throws Exception {
        mockMvc.perform(post("/api/feedback")
                .header("Authorization", tokenFor(alice.getEmail(), Role.CUSTOMER))
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());

        assertThat(feedbackRepository.count()).isZero();
    }

    @Test
    @DisplayName("Deleting a user should delete their feedback")
    void shouldDeleteFeedbackWithUser() throws Exception {
        mockMvc.perform(post("/api/feedback")
                .header("Authorization", tokenFor(alice.getEmail(), Role.CUSTOMER))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(alice.getId(), "Great service!", 5)))
            .andExpect(status().isCreated());

        userRepository.delete(alice);
        userRepository.flush();

        assertThat(feedbackRepository.count()).isZero();
    }
}
