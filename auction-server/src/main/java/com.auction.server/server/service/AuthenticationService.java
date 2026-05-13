package com.auction.server.service;

import com.auction.model.Bidder;
import com.auction.model.Seller;
import com.auction.model.User;
import com.auction.server.dao.UserDao;
import com.auction.shared.enums.UserRole;
import com.auction.shared.protocol.RegisterRequest;

public final class AuthenticationService {
    private final UserDao userDao;

    public AuthenticationService(UserDao userDao) {
        this.userDao = userDao;
    }

    // Xac thuc tai khoan dang nhap bang username/password da luu trong UserDao.
    public User login(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required.");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password is required.");
        }

        User user = userDao.findById(username.trim())
                .orElseThrow(() -> new IllegalArgumentException("Unknown username: " + username));
        if (!user.getPassword().equals(password.trim())) {
            throw new IllegalArgumentException("Invalid password.");
        }
        return user;
    }

    // Tao tai khoan moi tu request register va dua user vao bo nho server.
    public User register(RegisterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Register request is required.");
        }

        String username = requireText(request.username(), "Username is required.");
        String password = requireText(request.password(), "Password is required.");
        String displayName = requireText(request.displayName(), "Display name is required.");
        UserRole role = request.role() == null ? UserRole.BIDDER : request.role();

        if (password.length() < 4) {
            throw new IllegalArgumentException("Password must be at least 4 characters.");
        }
        if (role == UserRole.ADMIN) {
            throw new IllegalArgumentException("Admin accounts cannot self-register.");
        }
        if (userDao.findById(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }
        if (role == UserRole.SELLER && isBlank(request.storefrontName())) {
            throw new IllegalArgumentException("Seller accounts need a storefront name.");
        }

        String visibleName = role == UserRole.SELLER
                ? requireText(request.storefrontName(), "Seller accounts need a storefront name.")
                : displayName;
        User user = switch (role) {
            case SELLER -> new Seller(username, visibleName, password);
            case BIDDER -> new Bidder(username, displayName, password);
            case ADMIN -> throw new IllegalArgumentException("Admin accounts cannot self-register.");
        };
        userDao.save(user);
        return user;
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
