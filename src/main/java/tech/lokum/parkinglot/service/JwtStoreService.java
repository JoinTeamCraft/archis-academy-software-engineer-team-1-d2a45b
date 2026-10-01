package tech.lokum.parkinglot.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class JwtStoreService {

    private final Map<String, String> tokenStore = new ConcurrentHashMap<>();
    private final Map<String, String> usernameToRefreshToken = new ConcurrentHashMap<>();

    // Store access token
    public void storeAccessToken(String username, String token) {
        tokenStore.put(username, token);
    }

    // Get access token
    public String getAccessToken(String username) {
        return tokenStore.get(username);
    }

    // Store refresh token
    public void storeRefreshToken(String username, String token) {
        tokenStore.put(username + "_refresh", token);
    }

    // Get refresh token
    public String getRefreshToken(String username) {
        return tokenStore.get(username + "_refresh");
    }

    // Remove access token
    public void removeAccessToken(String username) {
        tokenStore.remove(username);
    }

    // Remove refresh token
    public void removeRefreshToken(String username) {
        tokenStore.remove(username + "_refresh");
    }

    // Remove all tokens (logout)
    public void removeAllTokens(String username) {
        removeAccessToken(username);
        removeRefreshToken(username);
    }

    // Check if access token exists
    public boolean accessTokenExists(String username) {
        return tokenStore.containsKey(username);
    }

    // Check if refresh token exists
    public boolean refreshTokenExists(String username) {
        return tokenStore.containsKey(username + "_refresh");
    }

    // Get all users with tokens
    public Set<String> getAllUsers() {
        return tokenStore.keySet();
    }

    public void revokeToken(String token) {

        tokenStore.entrySet()
                .removeIf(entry -> entry.getValue().equals(token));
    }
}
