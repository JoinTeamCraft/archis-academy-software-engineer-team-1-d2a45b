package tech.lokum.parkinglot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tech.lokum.parkinglot.dto.LoginRequest;
import tech.lokum.parkinglot.dto.LoginResponse;
import tech.lokum.parkinglot.entity.User;
import tech.lokum.parkinglot.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {

            throw new RuntimeException("Invalid credentials");
        }

        String token = jwtService.generateToken(
                user.getEmail());

        return new LoginResponse(
                token,
                user.getEmail(),
                user.getEmail(),
                user.getRole().name());
    }
}