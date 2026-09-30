package tech.lokum.parkinglot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Service handling feedback users submit about the application or service.
 */
@Service
public class FeedbackService {

    static final int MIN_RATING = 1;
    static final int MAX_RATING = 5;
    static final int MAX_FEEDBACK_LENGTH = 1000;

    private static final Logger log = LoggerFactory.getLogger(FeedbackService.class);

    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;

    public FeedbackService(FeedbackRepository feedbackRepository, UserRepository userRepository) {
        this.feedbackRepository = feedbackRepository;
        this.userRepository = userRepository;
    }

    /**
     * Validates and saves feedback for a user.
     *
     * @param request feedback payload
     * @param caller the authenticated caller; must be the user in the request, or an admin
     * @return saved feedback
     * @throws ValidationException if the rating is not between 1 and 5 or the text is blank or too long
     * @throws ResourceNotFoundException if the user does not exist
     * @throws ForbiddenException if the caller submits feedback on behalf of another user
     */
    @Transactional
    public FeedbackResponse submitFeedback(CreateFeedbackRequest request, Authentication caller) {
        validate(request);

        User user = userRepository.findById(request.userId())
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.userId()));

        if (!isOwnerOrAdmin(user, caller)) {
            throw new ForbiddenException("You can only submit feedback as yourself");
        }

        Feedback saved = feedbackRepository.save(
            new Feedback(user, request.feedbackText().strip(), request.rating())
        );
        log.info("User {} submitted feedback {} with rating {}", user.getId(), saved.getId(), saved.getRating());
        return FeedbackResponse.fromEntity(saved);
    }

    private void validate(CreateFeedbackRequest request) {
        List<String> errors = new ArrayList<>();
        if (request.userId() == null) {
            errors.add("User ID is required");
        }
        if (request.rating() == null || request.rating() < MIN_RATING || request.rating() > MAX_RATING) {
            errors.add(String.format("Rating must be between %d and %d", MIN_RATING, MAX_RATING));
        }
        String text = request.feedbackText();
        if (text == null || text.isBlank()) {
            errors.add("Feedback text is required");
        } else if (text.strip().length() > MAX_FEEDBACK_LENGTH) {
            errors.add(String.format("Feedback text must be at most %d characters", MAX_FEEDBACK_LENGTH));
        }
        if (!errors.isEmpty()) {
            throw new ValidationException("Invalid feedback", errors);
        }
    }

    private boolean isOwnerOrAdmin(User user, Authentication caller) {
        if (caller == null) {
            return false;
        }
        boolean isAdmin = caller.getAuthorities().stream()
            .anyMatch(authority -> Role.ADMIN.getAuthority().equals(authority.getAuthority()));
        return isAdmin || user.getEmail().equalsIgnoreCase(caller.getName());
    }
}
