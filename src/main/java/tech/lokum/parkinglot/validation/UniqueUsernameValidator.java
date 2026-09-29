package tech.lokum.parkinglot.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import tech.lokum.parkinglot.repository.UserRepository;

/**
 * Validates uniqueness of username against the database.
 */
@Component
public class UniqueUsernameValidator implements ConstraintValidator<UniqueUsername, String> {

    private final ObjectProvider<UserRepository> userRepositoryProvider;

    public UniqueUsernameValidator(ObjectProvider<UserRepository> userRepositoryProvider) {
        this.userRepositoryProvider = userRepositoryProvider;
    }

    @Override
    public boolean isValid(String username, ConstraintValidatorContext context) {
        if (username == null || username.isBlank()) {
            return true; // Let @NotBlank handle blank checks
        }

        UserRepository userRepository = userRepositoryProvider.getIfAvailable();
        if (userRepository == null) {
            return true;
        }

        String trimmed = username.trim();
        return !userRepository.existsByUsernameIgnoreCase(trimmed)
                && !userRepository.existsByEmailIgnoreCase(trimmed);
    }
}
