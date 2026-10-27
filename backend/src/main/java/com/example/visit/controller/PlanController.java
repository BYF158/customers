package com.example.visit.controller;

import com.example.visit.common.ApiResponse;
import com.example.visit.common.PageResult;
import com.example.visit.dto.Dtos.PlanCreateReq;
import com.example.visit.dto.Dtos.PlanUpdateReq;
import com.example.visit.dto.Views.PlanView;
import com.example.visit.security.AdminOnly;
import com.example.visit.security.CurrentUser;
import com.example.visit.security.UserContext;
import com.example.visit.service.PlanService;
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
@RequestMapping("/api/plans")
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @GetMapping
    public ApiResponse<PageResult<PlanView>> search(
            @RequestParam(required = false) Long managerId,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        CurrentUser current = UserContext.get();
        return ApiResponse.ok(planService.search(current, managerId, month, status, keyword, page, size));
    }

    @GetMapping("/months")
    public ApiResponse<List<String>> months() {
        return ApiResponse.ok(planService.months(UserContext.get()));
    }

    @GetMapping("/{id}")
    public ApiResponse<PlanView> get(@PathVariable long id) {
        return ApiResponse.ok(planService.get(UserContext.get(), id));
    }

    @AdminOnly
    @PostMapping
    public ApiResponse<Long> create(@Valid @RequestBody PlanCreateReq req) {
        return ApiResponse.ok(planService.create(req));
    }

    @AdminOnly
    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable long id, @Valid @RequestBody PlanUpdateReq req) {
        planService.update(id, req);
        return ApiResponse.ok();
    }

    @AdminOnly
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable long id) {
        planService.delete(id);
        return ApiResponse.ok();
    }
}
