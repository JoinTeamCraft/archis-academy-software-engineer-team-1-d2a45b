package tech.lokum.parkinglot.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import tech.lokum.parkinglot.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UniqueEmailValidatorTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ObjectProvider<UserRepository> userRepositoryProvider;

    private UniqueEmailValidator validator;

    @BeforeEach
    void setUp() {
        lenient().when(userRepositoryProvider.getIfAvailable()).thenReturn(userRepository);
        validator = new UniqueEmailValidator(userRepositoryProvider);
    }

    @Test
    @DisplayName("Should pass when email is null or blank (delegated to @NotBlank)")
    void shouldPassWhenNullOrBlank() {
        assertThat(validator.isValid(null, null)).isTrue();
        assertThat(validator.isValid("", null)).isTrue();
        assertThat(validator.isValid("   ", null)).isTrue();
    }

    @Test
    @DisplayName("Should pass when email does not exist in repository")
    void shouldPassWhenEmailIsUnique() {
        when(userRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);

        assertThat(validator.isValid("new@example.com", null)).isTrue();
    }

    @Test
    @DisplayName("Should fail when email already exists")
    void shouldFailWhenEmailExists() {
        when(userRepository.existsByEmailIgnoreCase("existing@example.com")).thenReturn(true);

        assertThat(validator.isValid("existing@example.com", null)).isFalse();
    }
}
