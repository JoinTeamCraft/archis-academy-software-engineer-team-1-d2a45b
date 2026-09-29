package tech.lokum.parkinglot.exception;

import java.util.Collections;
import java.util.List;

/**
 * Exception thrown when business or format validation rules fail.
 */
public class ValidationException extends RuntimeException {

    private final List<String> details;

    public ValidationException(String message, List<String> details) {
        super(message);
        this.details = details != null ? details : Collections.emptyList();
    }

    public ValidationException(List<String> details) {
        super("Validation failed");
        this.details = details != null ? details : Collections.emptyList();
    }

    public ValidationException(String detail) {
        super("Validation failed");
        this.details = detail != null ? List.of(detail) : Collections.emptyList();
    }

    public List<String> getDetails() {
        return details;
    }
}
