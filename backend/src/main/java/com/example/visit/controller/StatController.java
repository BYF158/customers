package com.example.visit.controller;

import com.example.visit.common.ApiResponse;
import com.example.visit.dto.Views.GroupStat;
import com.example.visit.dto.Views.HomeSummary;
import com.example.visit.dto.Views.ManagerStat;
import com.example.visit.dto.Views.MonthStat;
import com.example.visit.dto.Views.Overview;
import com.example.visit.security.UserContext;
import com.example.visit.service.StatService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stats")
public class StatController {

    private final StatService statService;

    public StatController(StatService statService) {
        this.statService = statService;
    }

    @GetMapping("/home")
    public ApiResponse<HomeSummary> home(@RequestParam(required = false) String month) {
        return ApiResponse.ok(statService.home(UserContext.get(), month));
    }

    @GetMapping("/overview")
    public ApiResponse<Overview> overview(@RequestParam(required = false) Long managerId,
                                          @RequestParam(required = false) String month) {
        return ApiResponse.ok(statService.overview(UserContext.get(), managerId, month));
    }

    @GetMapping("/by-manager")
    public ApiResponse<List<ManagerStat>> byManager(@RequestParam(required = false) String month) {
        return ApiResponse.ok(statService.byManager(UserContext.get(), month));
    }

    @GetMapping("/by-month")
    public ApiResponse<List<MonthStat>> byMonth(@RequestParam(required = false) Long managerId,
                                                @RequestParam(required = false) String year) {
        return ApiResponse.ok(statService.byMonth(UserContext.get(), managerId, year));
    }

    @GetMapping("/by-group")
    public ApiResponse<List<GroupStat>> byGroup(@RequestParam(required = false) Long managerId,
                                                @RequestParam(required = false) String month) {
        return ApiResponse.ok(statService.byGroup(UserContext.get(), managerId, month));
    }
}
