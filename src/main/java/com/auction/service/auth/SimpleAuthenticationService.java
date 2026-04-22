package com.auction.service.auth;

import java.util.Map;

import com.auction.exception.AuthenticationException;

public class SimpleAuthenticationService implements AuthenticationService {
    private static final Map<String, String> ACCOUNT_STORE = Map.of(
            "admin", "admin123",
            "minh", "123456",
            "an", "123456"
    );

    @Override
    public void authenticate(String username, String password) {
        if (username == null || password == null) {
            throw new AuthenticationException("Username/password is required");
        }

        String normalizedUsername = username.trim().toLowerCase();
        String normalizedPassword = password.trim();

        if (normalizedUsername.isEmpty() || normalizedPassword.isEmpty()) {
            throw new AuthenticationException("Username/password is required");
        }

        String expectedPassword = ACCOUNT_STORE.get(normalizedUsername);
        if (expectedPassword == null || !expectedPassword.equals(normalizedPassword)) {
            throw new AuthenticationException("Invalid username or password");
        }
    }
}
