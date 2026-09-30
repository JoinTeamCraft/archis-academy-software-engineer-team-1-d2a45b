package tech.lokum.parkinglot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.lokum.parkinglot.dto.CreateFeedbackRequest;
import tech.lokum.parkinglot.dto.ErrorResponse;
import tech.lokum.parkinglot.dto.FeedbackResponse;
import tech.lokum.parkinglot.service.FeedbackService;

/**
 * REST controller for submitting feedback about the application or service.
 */
@RestController
@RequestMapping("/api/feedback")
@Tag(name = "Feedback", description = "User feedback about the application or service")
@SecurityRequirement(name = "bearerAuth")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    /**
     * Submits feedback for the authenticated user.
     *
     * @param request feedback payload
     * @param authentication the authenticated caller
     * @return 201 Created with the saved feedback
     */
    @PostMapping
    @Operation(
        summary = "Submit feedback",
        description = "Saves feedback about the application or service. The rating must be between 1 and 5. "
            + "Users can only submit feedback as themselves; admins can submit on behalf of any user."
    )
    @ApiResponse(responseCode = "201", description = "Feedback submitted successfully",
        content = @Content(schema = @Schema(implementation = FeedbackResponse.class)))
    @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Submitting feedback on behalf of another user", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "User not found", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<FeedbackResponse> submitFeedback(
        @Valid @RequestBody CreateFeedbackRequest request,
        Authentication authentication
    ) {
        FeedbackResponse created = feedbackService.submitFeedback(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
