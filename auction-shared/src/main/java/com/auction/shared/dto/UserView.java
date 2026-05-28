package com.auction.shared.dto;

import com.auction.shared.enums.UserRole;

import java.io.Serializable;

public record UserView(
        String id,
        String name,
        UserRole role) implements Serializable {

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public UserRole getRole() {
        return role;
    }

    // compatibility aliases used by client
    public String getDisplayName() { return name; }
    public String getRoleLabel() { return role == null ? "" : role.name(); }
    public String getUsername() { return id; }
    public double getBalance() { return 0.0; }
}
