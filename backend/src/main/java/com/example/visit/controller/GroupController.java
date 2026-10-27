package com.example.visit.controller;

import com.example.visit.common.ApiResponse;
import com.example.visit.dto.Dtos.GroupReq;
import com.example.visit.dto.Views.GroupView;
import com.example.visit.security.AdminOnly;
import com.example.visit.service.GroupService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping
    public ApiResponse<List<GroupView>> list(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(groupService.list(keyword));
    }

    @AdminOnly
    @PostMapping
    public ApiResponse<Long> create(@Valid @RequestBody GroupReq req) {
        return ApiResponse.ok(groupService.create(req));
    }

    @AdminOnly
    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable long id, @Valid @RequestBody GroupReq req) {
        groupService.update(id, req);
        return ApiResponse.ok();
    }

    @AdminOnly
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable long id) {
        groupService.delete(id);
        return ApiResponse.ok();
    }
}
