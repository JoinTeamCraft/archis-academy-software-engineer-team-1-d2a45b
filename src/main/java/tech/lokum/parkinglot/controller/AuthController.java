package tech.lokum.parkinglot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.lokum.parkinglot.dto.LoginRequest;
import tech.lokum.parkinglot.dto.LoginResponse;
import tech.lokum.parkinglot.entity.User;
import tech.lokum.parkinglot.exception.ResourceNotFoundException;
import tech.lokum.parkinglot.repository.UserRepository;
import tech.lokum.parkinglot.security.JwtService;

/**
 * Controller providing authentication endpoints (login) to issue JWT tokens with roles.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication and token issuance endpoints")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UserRepository userRepository
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user", description = "Validates user credentials and returns a signed JWT containing roles.")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().trim(), request.password())
        );

        User user = userRepository.findByEmail(request.email().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.email()));

        String token = jwtService.generateToken(user.getEmail(), user.getRole());

        return ResponseEntity.ok(new LoginResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getRole(),
                86400000L
        ));
    }
}

