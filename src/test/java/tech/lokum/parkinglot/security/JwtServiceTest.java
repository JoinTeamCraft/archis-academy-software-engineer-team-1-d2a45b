package tech.lokum.parkinglot.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import tech.lokum.parkinglot.entity.Role;
import tech.lokum.parkinglot.entity.User;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private JwtService jwtService;
    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long EXPIRATION_MS = 3600000;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, EXPIRATION_MS);
    }

    @Test
    @DisplayName("generateToken with Role should include username and role claim")
    void testGenerateTokenWithRole() {
        String token = jwtService.generateToken("admin@example.com", Role.ADMIN);

        assertNotNull(token);
        assertEquals("admin@example.com", jwtService.extractUsername(token));
        assertEquals("ADMIN", jwtService.extractRole(token));

        List<GrantedAuthority> authorities = jwtService.extractAuthorities(token);
        assertEquals(1, authorities.size());
        assertEquals("ROLE_ADMIN", authorities.get(0).getAuthority());
    }

    @Test
    @DisplayName("generateToken with String role should extract authority correctly")
    void testGenerateTokenWithStringRole() {
        String token = jwtService.generateToken("manager@example.com", "MANAGER");

        assertNotNull(token);
        assertEquals("manager@example.com", jwtService.extractUsername(token));
        assertEquals("MANAGER", jwtService.extractRole(token));

        List<GrantedAuthority> authorities = jwtService.extractAuthorities(token);
        assertEquals(1, authorities.size());
        assertEquals("ROLE_MANAGER", authorities.get(0).getAuthority());
    }

    @Test
    @DisplayName("validateToken against UserDetails should return true for matching user")
    void testValidateTokenWithUserDetails() {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setRole(Role.USER);
        user.setPassword("hashed_secret");
        user.setActive(true);

        UserDetails userDetails = new UserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPassword(),
                user.getRole(),
                user.isActive()
        );

        String token = jwtService.generateToken(userDetails);

        assertTrue(jwtService.validateToken(token, userDetails));
        assertTrue(jwtService.validateToken(token));
        assertEquals("USER", jwtService.extractRole(token));
    }

    @Test
    @DisplayName("validateToken against wrong user should return false")
    void testValidateTokenAgainstWrongUser() {
        User user1 = new User();
        user1.setId(1L);
        user1.setEmail("user1@example.com");
        user1.setRole(Role.USER);

        UserDetails userDetails1 = new UserPrincipal(1L, "user1@example.com", "hash", Role.USER, true);
        UserDetails userDetails2 = new UserPrincipal(2L, "user2@example.com", "hash", Role.USER, true);

        String token = jwtService.generateToken(userDetails1);

        assertFalse(jwtService.validateToken(token, userDetails2));
    }

    @Test
    @DisplayName("validateToken with invalid signature should return false")
    void testValidateTokenInvalidSignature() {
        String token = jwtService.generateToken("user@example.com", Role.USER);
        String tamperedToken = token + "corrupted";

        assertFalse(jwtService.validateToken(tamperedToken));
    }

    @Test
    @DisplayName("isTokenExpired should return true for expired token")
    void testExpiredToken() {
        JwtService shortLivedJwtService = new JwtService(SECRET, -1000);
        String token = shortLivedJwtService.generateToken("expired@example.com", Role.USER);

        assertTrue(shortLivedJwtService.isTokenExpired(token));
        assertFalse(shortLivedJwtService.validateToken(token));
    }
}
