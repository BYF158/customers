package com.example.visit.service;

import com.example.visit.common.Periods;
import com.example.visit.dto.Views.GroupStat;
import com.example.visit.dto.Views.HomeSummary;
import com.example.visit.dto.Views.ManagerStat;
import com.example.visit.dto.Views.MonthStat;
import com.example.visit.dto.Views.Overview;
import com.example.visit.dto.Views.PlanView;
import com.example.visit.repo.PlanRepository;
import com.example.visit.repo.StatRepository;
import com.example.visit.repo.UserRepository;
import com.example.visit.security.CurrentUser;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class StatService {

    private final StatRepository stats;
    private final PlanRepository plans;
    private final UserRepository users;

    public StatService(StatRepository stats, PlanRepository plans, UserRepository users) {
        this.stats = stats;
        this.plans = plans;
        this.users = users;
    }

    public Overview overview(CurrentUser current, Long managerId, String month) {
        return stats.overview(scope(current, managerId), monthOrNull(month));
    }

    public List<ManagerStat> byManager(CurrentUser current, String month) {
        return stats.byManager(current.isAdmin() ? null : current.id(), monthOrNull(month));
    }

    public List<MonthStat> byMonth(CurrentUser current, Long managerId, String year) {
        String targetYear = (year == null || year.isBlank())
                ? String.valueOf(java.time.LocalDate.now().getYear())
                : year.trim();
        if (!targetYear.matches("\\d{4}")) {
            throw com.example.visit.common.BizException.badRequest("年份格式不正确，应为 YYYY");
        }
        List<MonthStat> rows = stats.byMonth(scope(current, managerId), targetYear);
        // 补齐 12 个月，方便前端画完整柱状图
        List<MonthStat> filled = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            String key = String.format("%s-%02d", targetYear, m);
            MonthStat found = rows.stream().filter(r -> key.equals(r.month())).findFirst().orElse(null);
            filled.add(found == null ? new MonthStat(key, 0, 0, 0) : found);
        }
        return filled;
    }

    public List<GroupStat> byGroup(CurrentUser current, Long managerId, String month) {
        return stats.byGroup(scope(current, managerId), monthOrNull(month));
    }

    public HomeSummary home(CurrentUser current, String month) {
        Long scope = current.isAdmin() ? null : current.id();
        String targetMonth = monthOrNull(month);
        Overview overview = stats.overview(scope, targetMonth);
        String year = targetMonth == null
                ? String.valueOf(java.time.LocalDate.now().getYear())
                : String.valueOf(Periods.yearOf(targetMonth));
        List<MonthStat> months = byMonth(current, scope, year);
        List<PlanView> todo = plans.findTodo(scope, targetMonth, 5);
        long managerCount = current.isAdmin() ? users.countByRole("MANAGER") : 1;
        return new HomeSummary(overview, months, todo, managerCount);
    }

    private Long scope(CurrentUser current, Long requested) {
        return current.isAdmin() ? requested : current.id();
    }

    private String monthOrNull(String month) {
        return (month == null || month.isBlank()) ? null : Periods.requireMonth(month);
    }
}
