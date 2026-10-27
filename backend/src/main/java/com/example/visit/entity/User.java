package com.example.visit.entity;

import java.time.LocalDateTime;

public record User(
        Long id,
        String username,
        String realName,
        String password,
        String role,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_MANAGER = "MANAGER";

    public boolean isAdmin() {
        return ROLE_ADMIN.equals(role);
    }
}
