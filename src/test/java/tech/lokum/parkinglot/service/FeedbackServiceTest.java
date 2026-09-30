package tech.lokum.parkinglot.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import tech.lokum.parkinglot.dto.CreateFeedbackRequest;
import tech.lokum.parkinglot.dto.FeedbackResponse;
import tech.lokum.parkinglot.entity.Feedback;
import tech.lokum.parkinglot.entity.Role;
import tech.lokum.parkinglot.entity.User;
import tech.lokum.parkinglot.exception.ForbiddenException;
import tech.lokum.parkinglot.exception.ResourceNotFoundException;
import tech.lokum.parkinglot.exception.ValidationException;
import tech.lokum.parkinglot.repository.FeedbackRepository;
import tech.lokum.parkinglot.repository.UserRepository;
import tech.lokum.parkinglot.security.UserPrincipal;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    private static final Instant SUBMITTED_AT = Instant.parse("2025-01-23T10:00:00Z");

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FeedbackService feedbackService;

    private User alice;

    @BeforeEach
    void setUp() {
        alice = new User("alice@test.com", "hashed", "Alice", null, Role.CUSTOMER);
        alice.setId(1L);
    }

    private static Authentication caller(String email, Role role) {
        return new UsernamePasswordAuthenticationToken(email, null, List.of(new SimpleGrantedAuthority(role.getAuthority())));
    }

    private static Authentication principalCaller(Long id, String email, Role role) {
        UserPrincipal principal = new UserPrincipal(id, email, "hashed", role, true);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    private void stubSave() {
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(invocation -> {
            Feedback feedback = invocation.getArgument(0);
            feedback.setId(101L);
            feedback.setCreatedAt(SUBMITTED_AT);
            return feedback;
        });
    }

    private static List<String> detailsOf(Throwable ex) {
        return ((ValidationException) ex).getDetails();
    }

    @Test
    @DisplayName("submitFeedback should save the feedback and return it with id and submittedAt")
    void shouldSaveFeedback() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        stubSave();

        FeedbackResponse response = feedbackService.submitFeedback(
            new CreateFeedbackRequest(1L, "Great service!", 5), caller("alice@test.com", Role.CUSTOMER));

        assertThat(response).isEqualTo(new FeedbackResponse(101L, 1L, "Great service!", 5, SUBMITTED_AT));
    }

    @Test
    @DisplayName("submitFeedback should trim surrounding whitespace from the feedback text")
    void shouldTrimFeedbackText() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        stubSave();

        feedbackService.submitFeedback(
            new CreateFeedbackRequest(1L, "  Great service!\n", 4), caller("alice@test.com", Role.CUSTOMER));

        ArgumentCaptor<Feedback> saved = ArgumentCaptor.forClass(Feedback.class);
        verify(feedbackRepository).save(saved.capture());
        assertThat(saved.getValue().getFeedbackText()).isEqualTo("Great service!");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5})
    @DisplayName("submitFeedback should accept the lowest and highest ratings")
    void shouldAcceptBoundaryRatings(int rating) {
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        stubSave();

        FeedbackResponse response = feedbackService.submitFeedback(
            new CreateFeedbackRequest(1L, "Okay", rating), caller("alice@test.com", Role.CUSTOMER));

        assertThat(response.rating()).isEqualTo(rating);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 6, -1})
    @DisplayName("submitFeedback should reject ratings outside 1 to 5")
    void shouldRejectOutOfRangeRating(int rating) {
        CreateFeedbackRequest request = new CreateFeedbackRequest(1L, "Okay", rating);

        assertThatThrownBy(() -> feedbackService.submitFeedback(request, caller("alice@test.com", Role.CUSTOMER)))
            .isInstanceOf(ValidationException.class)
            .satisfies(ex -> assertThat(detailsOf(ex)).containsExactly("Rating must be between 1 and 5"));
        verifyNoInteractions(userRepository, feedbackRepository);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\n\t"})
    @DisplayName("submitFeedback should reject missing or blank feedback text")
    void shouldRejectBlankText(String text) {
        CreateFeedbackRequest request = new CreateFeedbackRequest(1L, text, 3);

        assertThatThrownBy(() -> feedbackService.submitFeedback(request, caller("alice@test.com", Role.CUSTOMER)))
            .isInstanceOf(ValidationException.class)
            .satisfies(ex -> assertThat(detailsOf(ex)).containsExactly("Feedback text is required"));
        verifyNoInteractions(feedbackRepository);
    }

    @Test
    @DisplayName("submitFeedback should reject feedback text longer than 1000 characters")
    void shouldRejectTooLongText() {
        CreateFeedbackRequest request = new CreateFeedbackRequest(1L, "a".repeat(1001), 3);

        assertThatThrownBy(() -> feedbackService.submitFeedback(request, caller("alice@test.com", Role.CUSTOMER)))
            .isInstanceOf(ValidationException.class)
            .satisfies(ex -> assertThat(detailsOf(ex)).containsExactly("Feedback text must be at most 1000 characters"));
    }

    @Test
    @DisplayName("submitFeedback should report every invalid field at once")
    void shouldReportAllErrors() {
        CreateFeedbackRequest request = new CreateFeedbackRequest(null, " ", null);

        assertThatThrownBy(() -> feedbackService.submitFeedback(request, caller("alice@test.com", Role.CUSTOMER)))
            .isInstanceOf(ValidationException.class)
            .satisfies(ex -> assertThat(detailsOf(ex)).containsExactly(
                "User ID is required", "Rating is required", "Feedback text is required"));
    }

    @Test
    @DisplayName("submitFeedback should throw ResourceNotFoundException when the user does not exist")
    void shouldRejectUnknownUser() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        CreateFeedbackRequest request = new CreateFeedbackRequest(99L, "Great service!", 5);

        assertThatThrownBy(() -> feedbackService.submitFeedback(request, caller("admin@test.com", Role.ADMIN)))
            .isInstanceOf(ResourceNotFoundException.class);
        verify(feedbackRepository, never()).save(any());
    }

    @Test
    @DisplayName("submitFeedback should forbid a user from submitting feedback as someone else")
    void shouldForbidOtherUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        CreateFeedbackRequest request = new CreateFeedbackRequest(1L, "Great service!", 5);

        assertThatThrownBy(() -> feedbackService.submitFeedback(request, caller("mallory@test.com", Role.CUSTOMER)))
            .isInstanceOf(ForbiddenException.class);
        verify(feedbackRepository, never()).save(any());
    }

    @Test
    @DisplayName("submitFeedback should forbid an anonymous caller before looking up the user")
    void shouldForbidAnonymousCaller() {
        CreateFeedbackRequest request = new CreateFeedbackRequest(1L, "Great service!", 5);

        assertThatThrownBy(() -> feedbackService.submitFeedback(request, null))
            .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(userRepository, feedbackRepository);
    }

    @Test
    @DisplayName("submitFeedback should forbid an anonymous caller even when the body is invalid")
    void shouldForbidAnonymousCallerBeforeValidating() {
        CreateFeedbackRequest request = new CreateFeedbackRequest(null, " ", 9);

        assertThatThrownBy(() -> feedbackService.submitFeedback(request, null))
            .isInstanceOf(ForbiddenException.class);
        verifyNoInteractions(userRepository, feedbackRepository);
    }

    @Test
    @DisplayName("submitFeedback should report a missing rating separately from an out-of-range one")
    void shouldRejectMissingRating() {
        CreateFeedbackRequest request = new CreateFeedbackRequest(1L, "Okay", null);

        assertThatThrownBy(() -> feedbackService.submitFeedback(request, caller("alice@test.com", Role.CUSTOMER)))
            .isInstanceOf(ValidationException.class)
            .satisfies(ex -> assertThat(detailsOf(ex)).containsExactly("Rating is required"));
    }

    @Test
    @DisplayName("submitFeedback should let an admin submit feedback on behalf of a user")
    void shouldAllowAdmin() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        stubSave();

        FeedbackResponse response = feedbackService.submitFeedback(
            new CreateFeedbackRequest(1L, "Reported by phone", 2), caller("admin@test.com", Role.ADMIN));

        assertThat(response.userId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("submitFeedback should match the caller's email case-insensitively")
    void shouldMatchEmailIgnoringCase() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        stubSave();

        FeedbackResponse response = feedbackService.submitFeedback(
            new CreateFeedbackRequest(1L, "Great service!", 5), caller("Alice@Test.com", Role.CUSTOMER));

        assertThat(response.id()).isEqualTo(101L);
    }

    @Test
    @DisplayName("submitFeedback should reject a missing request body")
    void shouldRejectNullRequest() {
        assertThatThrownBy(() -> feedbackService.submitFeedback(null, caller("alice@test.com", Role.CUSTOMER)))
            .isInstanceOf(ValidationException.class)
            .satisfies(ex -> assertThat(detailsOf(ex)).containsExactly("Request body is required"));
        verifyNoInteractions(userRepository, feedbackRepository);
    }

    @Test
    @DisplayName("submitFeedback should accept a caller whose loaded principal has the user's id")
    void shouldMatchPrincipalById() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        stubSave();

        FeedbackResponse response = feedbackService.submitFeedback(
            new CreateFeedbackRequest(1L, "Great service!", 5), principalCaller(1L, "alice@test.com", Role.CUSTOMER));

        assertThat(response.userId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("submitFeedback should forbid a loaded principal with another id, even if the email matches")
    void shouldForbidPrincipalWithOtherId() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(alice));
        CreateFeedbackRequest request = new CreateFeedbackRequest(1L, "Great service!", 5);

        assertThatThrownBy(() -> feedbackService.submitFeedback(request, principalCaller(2L, "alice@test.com", Role.CUSTOMER)))
            .isInstanceOf(ForbiddenException.class);
        verify(feedbackRepository, never()).save(any());
    }
}
