package com.example.visit.controller;

import com.example.visit.common.ApiResponse;
import com.example.visit.common.PageResult;
import com.example.visit.dto.Dtos.VisitCreateReq;
import com.example.visit.dto.Dtos.VisitUpdateReq;
import com.example.visit.dto.Views.VisitView;
import com.example.visit.security.UserContext;
import com.example.visit.service.VisitService;
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
@RequestMapping("/api/visits")
public class VisitController {

    private final VisitService visitService;

    public VisitController(VisitService visitService) {
        this.visitService = visitService;
    }

    @GetMapping
    public ApiResponse<PageResult<VisitView>> search(
            @RequestParam(required = false) Long managerId,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(visitService.search(UserContext.get(), managerId, month, keyword, page, size));
    }

    @GetMapping("/options")
    public ApiResponse<Options> options() {
        return ApiResponse.ok(new Options(VisitService.METHODS, VisitService.RESULTS));
    }

    @PostMapping
    public ApiResponse<Long> create(@Valid @RequestBody VisitCreateReq req) {
        return ApiResponse.ok(visitService.create(UserContext.get(), req));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable long id, @Valid @RequestBody VisitUpdateReq req) {
        visitService.update(UserContext.get(), id, req);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable long id) {
        visitService.delete(UserContext.get(), id);
        return ApiResponse.ok();
    }

    public record Options(List<String> methods, List<String> results) {
    }
}
