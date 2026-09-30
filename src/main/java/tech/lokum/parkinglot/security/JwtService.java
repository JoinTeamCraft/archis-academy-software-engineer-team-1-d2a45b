package tech.lokum.parkinglot.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import tech.lokum.parkinglot.entity.Role;

import java.security.Key;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service for generating, parsing, and validating JWT tokens with role-based claims.
 */
@Service
public class JwtService {

    private static final String DEFAULT_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    private final String secretKey;
    private final long expirationMs;

    public JwtService(
            @Value("${jwt.secret:" + DEFAULT_SECRET + "}") String secretKey,
            @Value("${jwt.expiration-ms:86400000}") long expirationMs
    ) {
        this.secretKey = secretKey;
        this.expirationMs = expirationMs;
    }

    private Key getSigningKey() {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secretKey);
        } catch (IllegalArgumentException e) {
            keyBytes = secretKey.getBytes();
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String username, Role role) {
        Map<String, Object> claims = new HashMap<>();
        String roleName = role != null ? role.name() : Role.USER.name();
        claims.put("role", roleName);
        claims.put("roles", List.of("ROLE_" + roleName));
        return createToken(claims, username);
    }

    public String generateToken(String username, String roleName) {
        Map<String, Object> claims = new HashMap<>();
        String sanitizedRole = roleName != null ? roleName.replace("ROLE_", "").toUpperCase() : Role.USER.name();
        claims.put("role", sanitizedRole);
        claims.put("roles", List.of("ROLE_" + sanitizedRole));
        return createToken(claims, username);
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        List<String> authorities = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        claims.put("roles", authorities);

        String role = authorities.stream()
                .filter(auth -> auth.startsWith("ROLE_"))
                .map(auth -> auth.substring(5))
                .findFirst()
                .orElse(Role.USER.name());
        claims.put("role", role);

        return createToken(claims, userDetails.getUsername());
    }

    public String generateToken(Map<String, Object> extraClaims, String username, long customExpirationMs) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + customExpirationMs);

        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractRole(String token) {
        Claims claims = extractAllClaims(token);
        String role = claims.get("role", String.class);
        if (role == null) {
            List<?> roles = claims.get("roles", List.class);
            if (roles != null && !roles.isEmpty()) {
                String first = roles.get(0).toString();
                return first.startsWith("ROLE_") ? first.substring(5) : first;
            }
        }
        return role;
    }

    public List<GrantedAuthority> extractAuthorities(String token) {
        Claims claims = extractAllClaims(token);
        List<?> roles = claims.get("roles", List.class);
        if (roles != null && !roles.isEmpty()) {
            return roles.stream()
                    .map(Object::toString)
                    .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        }
        String singleRole = claims.get("role", String.class);
        if (singleRole != null) {
            String roleName = singleRole.startsWith("ROLE_") ? singleRole : "ROLE_" + singleRole;
            return List.of(new SimpleGrantedAuthority(roleName));
        }
        return Collections.emptyList();
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return username != null && username.equalsIgnoreCase(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
