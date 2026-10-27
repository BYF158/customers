package com.example.visit.repo;

import com.example.visit.dto.Views.GroupStat;
import com.example.visit.dto.Views.ManagerStat;
import com.example.visit.dto.Views.MonthStat;
import com.example.visit.dto.Views.Overview;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class StatRepository {

    private final JdbcTemplate jdbc;

    public StatRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static int rate(long done, long total) {
        return total <= 0 ? 0 : (int) Math.round(done * 100.0 / total);
    }

    public Overview overview(Long managerId, String month) {
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(p.id) AS total,
                       COALESCE(SUM(CASE WHEN v.id IS NOT NULL THEN 1 ELSE 0 END), 0) AS done,
                       COALESCE(SUM(CASE WHEN v.id IS NOT NULL AND v.visit_date <=
                           LAST_DAY(STR_TO_DATE(CONCAT(p.plan_month, '-01'), '%Y-%m-%d')) THEN 1 ELSE 0 END), 0) AS on_time,
                       COUNT(DISTINCT p.group_id) AS group_count,
                       COUNT(v.id) AS visit_count
                FROM visit_plan p
                LEFT JOIN visit_record v ON v.plan_id = p.id
                WHERE 1 = 1
                """);
        List<Object> args = new ArrayList<>();
        if (managerId != null) {
            sql.append(" AND p.manager_id = ?");
            args.add(managerId);
        }
        if (month != null && !month.isBlank()) {
            sql.append(" AND p.plan_month = ?");
            args.add(month);
        }
        return jdbc.query(sql.toString(), rs -> {
            if (!rs.next()) {
                return new Overview(0, 0, 0, 0, 0, 0, 0);
            }
            long total = rs.getLong("total");
            long done = rs.getLong("done");
            long onTime = rs.getLong("on_time");
            long groupCount = rs.getLong("group_count");
            long visitCount = rs.getLong("visit_count");
            return new Overview(total, done, total - done, rate(done, total), onTime, groupCount, visitCount);
        }, args.toArray());
    }

    public List<ManagerStat> byManager(Long managerId, String month) {
        StringBuilder sql = new StringBuilder("""
                SELECT u.id, u.real_name AS manager_name,
                       COUNT(p.id) AS total,
                       COUNT(v.id) AS done
                FROM sys_user u
                LEFT JOIN visit_plan p ON p.manager_id = u.id
                """);
        List<Object> args = new ArrayList<>();
        if (month != null && !month.isBlank()) {
            sql.append(" AND p.plan_month = ?");
            args.add(month);
        }
        sql.append(" LEFT JOIN visit_record v ON v.plan_id = p.id WHERE u.role = 'MANAGER'");
        if (managerId != null) {
            sql.append(" AND u.id = ?");
            args.add(managerId);
        }
        sql.append(" GROUP BY u.id, u.real_name ORDER BY u.id");
        return jdbc.query(sql.toString(), (rs, i) -> {
            long total = rs.getLong("total");
            long done = rs.getLong("done");
            return new ManagerStat(rs.getLong("id"), rs.getString("manager_name"), total, done, rate(done, total));
        }, args.toArray());
    }

    public List<MonthStat> byMonth(Long managerId, String year) {
        StringBuilder sql = new StringBuilder("""
                SELECT p.plan_month AS month, COUNT(p.id) AS total, COUNT(v.id) AS done
                FROM visit_plan p
                LEFT JOIN visit_record v ON v.plan_id = p.id
                WHERE p.plan_month LIKE CONCAT(?, '-%')
                """);
        List<Object> args = new ArrayList<>();
        args.add(year);
        if (managerId != null) {
            sql.append(" AND p.manager_id = ?");
            args.add(managerId);
        }
        sql.append(" GROUP BY p.plan_month ORDER BY p.plan_month");
        List<MonthStat> rows = jdbc.query(sql.toString(), (rs, i) -> {
            long total = rs.getLong("total");
            long done = rs.getLong("done");
            return new MonthStat(rs.getString("month"), total, done, rate(done, total));
        }, args.toArray());
        return rows;
    }

    public List<GroupStat> byGroup(Long managerId, String month) {
        StringBuilder sql = new StringBuilder("""
                SELECT g.code, g.name,
                       GROUP_CONCAT(DISTINCT u.real_name ORDER BY u.real_name SEPARATOR '、') AS managers,
                       COUNT(p.id) AS total, COUNT(v.id) AS done
                FROM customer_group g
                JOIN visit_plan p ON p.group_id = g.id
                JOIN sys_user u ON u.id = p.manager_id
                LEFT JOIN visit_record v ON v.plan_id = p.id
                WHERE 1 = 1
                """);
        List<Object> args = new ArrayList<>();
        if (managerId != null) {
            sql.append(" AND p.manager_id = ?");
            args.add(managerId);
        }
        if (month != null && !month.isBlank()) {
            sql.append(" AND p.plan_month = ?");
            args.add(month);
        }
        sql.append(" GROUP BY g.id, g.code, g.name ORDER BY g.code");
        return jdbc.query(sql.toString(), (rs, i) -> new GroupStat(
                rs.getString("code"),
                rs.getString("name"),
                rs.getString("managers"),
                rs.getLong("total"),
                rs.getLong("done")), args.toArray());
    }
}
