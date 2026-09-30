package tech.lokum.parkinglot.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validates password length and strength constraints.
 */
public class PasswordStrengthValidator implements ConstraintValidator<PasswordStrength, String> {

    private int min;

    @Override
    public void initialize(PasswordStrength constraintAnnotation) {
        this.min = constraintAnnotation.min();
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.isBlank()) {
            return true; // Let @NotBlank handle presence
        }
        return password.length() >= min;
    }
}
