package com.example.visit.entity;

import java.time.LocalDateTime;

public record CustomerGroup(
        Long id,
        String code,
        String name,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
