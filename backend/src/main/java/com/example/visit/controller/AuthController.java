package com.example.visit.controller;

import com.example.visit.common.ApiResponse;
import com.example.visit.dto.Dtos.ChangePasswordReq;
import com.example.visit.dto.Dtos.LoginReq;
import com.example.visit.dto.Views.LoginResult;
import com.example.visit.dto.Views.UserView;
import com.example.visit.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResult> login(@Valid @RequestBody LoginReq req) {
        return ApiResponse.ok(authService.login(req));
    }

    @GetMapping("/me")
    public ApiResponse<UserView> me() {
        return ApiResponse.ok(authService.me());
    }

    @PostMapping("/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordReq req) {
        authService.changePassword(req);
        return ApiResponse.ok();
    }
}
