package tech.lokum.parkinglot.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PasswordStrengthValidatorTest {

    private PasswordStrengthValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PasswordStrengthValidator();
        PasswordStrength annotation = mock(PasswordStrength.class);
        when(annotation.min()).thenReturn(8);
        validator.initialize(annotation);
    }

    @Test
    @DisplayName("Should pass when password is null or blank (delegated to @NotBlank)")
    void shouldPassWhenNullOrBlank() {
        assertThat(validator.isValid(null, null)).isTrue();
        assertThat(validator.isValid("", null)).isTrue();
        assertThat(validator.isValid("   ", null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345678", "password123", "SuperSecurePassword!2026", "abcdefgh"})
    @DisplayName("Should pass when password has at least 8 characters")
    void shouldPassWhenLengthIsAtLeastEight(String password) {
        assertThat(validator.isValid(password, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "1234", "1234567", "short", "pass123"})
    @DisplayName("Should fail when password has fewer than 8 characters")
    void shouldFailWhenLengthIsLessThanEight(String password) {
        assertThat(validator.isValid(password, null)).isFalse();
    }
}
