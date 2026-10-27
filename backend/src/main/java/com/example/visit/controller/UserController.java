package com.example.visit.controller;

import com.example.visit.common.ApiResponse;
import com.example.visit.dto.Dtos.CreateUserReq;
import com.example.visit.dto.Dtos.ResetPasswordReq;
import com.example.visit.dto.Dtos.UpdateUserReq;
import com.example.visit.dto.Views.ManagerView;
import com.example.visit.dto.Views.UserView;
import com.example.visit.security.AdminOnly;
import com.example.visit.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@AdminOnly
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/managers")
    public ApiResponse<List<ManagerView>> managers() {
        return ApiResponse.ok(userService.listManagers());
    }

    @GetMapping("/options")
    public ApiResponse<List<UserView>> options() {
        return ApiResponse.ok(userService.options());
    }

    @PostMapping
    public ApiResponse<UserView> create(@Valid @RequestBody CreateUserReq req) {
        return ApiResponse.ok(userService.create(req));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable long id, @Valid @RequestBody UpdateUserReq req) {
        userService.update(id, req);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/password")
    public ApiResponse<Void> resetPassword(@PathVariable long id, @Valid @RequestBody ResetPasswordReq req) {
        userService.resetPassword(id, req.password());
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable long id) {
        userService.delete(id);
        return ApiResponse.ok();
    }
}
