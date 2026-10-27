package com.example.visit.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 响应视图集合。
 */
public final class Views {

    private Views() {
    }

    public record UserView(Long id, String username, String realName, String role, boolean enabled,
                           LocalDateTime createdAt) {
    }

    public record LoginResult(String token, UserView user) {
    }

    public record ManagerView(Long id, String username, String realName, String role, boolean enabled,
                              long planCount, long visitCount, LocalDateTime createdAt) {
    }

    public record GroupView(Long id, String code, String name, long planCount, LocalDateTime createdAt) {
    }

    public record PlanView(Long id,
                           Long managerId,
                           String managerName,
                           Long groupId,
                           String groupCode,
                           String groupName,
                           String planMonth,
                           String remark,
                           Long visitId,
                           LocalDate visitDate,
                           String method,
                           String result,
                           String visitRemark,
                           LocalDateTime createdAt) {
        public boolean isVisited() {
            return visitId != null;
        }
    }

    public record VisitView(Long id,
                            Long planId,
                            Long managerId,
                            String managerName,
                            String groupCode,
                            String groupName,
                            String planMonth,
                            LocalDate visitDate,
                            String method,
                            String result,
                            String remark,
                            LocalDateTime createdAt) {
    }

    public record Overview(long total, long done, long todo, int rate, long onTime,
                           long groupCount, long visitCount) {
    }

    public record ManagerStat(Long managerId, String managerName, long total, long done, int rate) {
    }

    public record MonthStat(String month, long total, long done, int rate) {
    }

    public record GroupStat(String code, String name, String managers, long total, long done) {
    }

    public record HomeSummary(Overview overview, List<MonthStat> months, List<PlanView> todoPlans,
                              long managerCount) {
    }

    public record ImportRowView(String managerName, String code, String name, String planMonth) {
    }

    public record ImportPreview(List<ImportRowView> rows, List<String> managers, List<String> months,
                                List<String> warnings, int ignoredSheets) {
    }

    public record CreatedManager(String name, String username, String password) {
    }

    public record ImportResult(int added, int skipped, List<CreatedManager> createdManagers,
                               List<String> warnings, String message) {
    }
}
