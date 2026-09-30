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
class UniqueUsernameValidatorTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ObjectProvider<UserRepository> userRepositoryProvider;

    private UniqueUsernameValidator validator;

    @BeforeEach
    void setUp() {
        lenient().when(userRepositoryProvider.getIfAvailable()).thenReturn(userRepository);
        validator = new UniqueUsernameValidator(userRepositoryProvider);
    }

    @Test
    @DisplayName("Should pass when username is null or blank (delegated to @NotBlank)")
    void shouldPassWhenNullOrBlank() {
        assertThat(validator.isValid(null, null)).isTrue();
        assertThat(validator.isValid("", null)).isTrue();
        assertThat(validator.isValid("   ", null)).isTrue();
    }

    @Test
    @DisplayName("Should pass when username does not exist in repository")
    void shouldPassWhenUsernameIsUnique() {
        when(userRepository.existsByUsernameIgnoreCase("new_user")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("new_user")).thenReturn(false);

        assertThat(validator.isValid("new_user", null)).isTrue();
    }

    @Test
    @DisplayName("Should fail when username already exists")
    void shouldFailWhenUsernameExists() {
        when(userRepository.existsByUsernameIgnoreCase("existing_user")).thenReturn(true);

        assertThat(validator.isValid("existing_user", null)).isFalse();
    }

    @Test
    @DisplayName("Should fail when username matches an existing user's email")
    void shouldFailWhenUsernameMatchesExistingEmail() {
        when(userRepository.existsByUsernameIgnoreCase("user@example.com")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("user@example.com")).thenReturn(true);

        assertThat(validator.isValid("user@example.com", null)).isFalse();
    }
}
