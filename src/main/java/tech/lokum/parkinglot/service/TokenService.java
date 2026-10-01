package tech.lokum.parkinglot.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;

import tech.lokum.parkinglot.entity.Token;
import tech.lokum.parkinglot.repository.TokenRepository;

public class TokenService {

    @Autowired
    private TokenRepository tokenRepository;

    public void storeRefreshToken(String username, String refreshToken, LocalDateTime expiry) {
        Token token = new Token();
        token.setUsername(username);
        token.setTokenValue(refreshToken);
        token.setTokenType("refresh");
        token.setExpiresAt(expiry);
        tokenRepository.save(token);
    }

    public boolean isValidRefreshToken(String refreshToken) {
        return tokenRepository.findByTokenValue(refreshToken)
                .filter(t -> !t.isRevoked() && t.getExpiresAt().isAfter(LocalDateTime.now()))
                .isPresent();
    }

    public void revokeTokensForUser(String username) {
        tokenRepository.findAll().stream()
                .filter(t -> t.getUsername().equals(username))
                .forEach(t -> {
                    t.setRevoked(true);
                    tokenRepository.save(t);
                });
    }

}
