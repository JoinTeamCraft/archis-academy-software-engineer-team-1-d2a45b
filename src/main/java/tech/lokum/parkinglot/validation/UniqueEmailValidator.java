package tech.lokum.parkinglot.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import tech.lokum.parkinglot.repository.UserRepository;

/**
 * Validates uniqueness of email against the database.
 */
@Component
public class UniqueEmailValidator implements ConstraintValidator<UniqueEmail, String> {

    private final ObjectProvider<UserRepository> userRepositoryProvider;

    public UniqueEmailValidator(ObjectProvider<UserRepository> userRepositoryProvider) {
        this.userRepositoryProvider = userRepositoryProvider;
    }

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        if (email == null || email.isBlank()) {
            return true;
        }

        UserRepository userRepository = userRepositoryProvider.getIfAvailable();
        if (userRepository == null) {
            return true;
        }

        return !userRepository.existsByEmailIgnoreCase(email.trim());
    }
}
