package com.example.visit.controller;

import com.example.visit.common.ApiResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final JdbcTemplate jdbc;

    public HealthController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> health() {
        Integer one = jdbc.queryForObject("SELECT 1", Integer.class);
        return ApiResponse.ok(Map.of("status", "UP", "db", one != null && one == 1 ? "UP" : "DOWN"));
    }
}
