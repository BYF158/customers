package com.example.visit.security;

public record CurrentUser(Long id, String username, String realName, String role) {

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
