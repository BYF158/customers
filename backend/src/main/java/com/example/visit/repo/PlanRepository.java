package com.example.visit.repo;

import com.example.visit.common.PageResult;
import com.example.visit.dto.Views.PlanView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class PlanRepository {

    private final JdbcTemplate jdbc;

    public PlanRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record PlanOwner(Long id, Long managerId, Long groupId, String planMonth) {
    }

    private static final String SELECT = """
            SELECT p.id, p.manager_id, u.real_name AS manager_name, p.group_id,
                   g.code AS group_code, g.name AS group_name, p.plan_month, p.remark, p.created_at,
                   v.id AS visit_id, v.visit_date, v.method, v.result,
                   v.remark AS visit_remark
            FROM visit_plan p
            JOIN sys_user u ON u.id = p.manager_id
            JOIN customer_group g ON g.id = p.group_id
            LEFT JOIN visit_record v ON v.plan_id = p.id
            """;

    private static final String COUNT = """
            SELECT COUNT(*)
            FROM visit_plan p
            JOIN customer_group g ON g.id = p.group_id
            LEFT JOIN visit_record v ON v.plan_id = p.id
            """;

    private static final org.springframework.jdbc.core.RowMapper<PlanView> MAPPER = (rs, i) -> new PlanView(
            rs.getLong("id"),
            rs.getLong("manager_id"),
            rs.getString("manager_name"),
            rs.getLong("group_id"),
            rs.getString("group_code"),
            rs.getString("group_name"),
            rs.getString("plan_month"),
            rs.getString("remark"),
            (Long) rs.getObject("visit_id", Long.class),
            rs.getDate("visit_date") == null ? null : rs.getDate("visit_date").toLocalDate(),
            rs.getString("method"),
            rs.getString("result"),
            rs.getString("visit_remark"),
            UserRepository.toTime(rs, "created_at"));

    public PageResult<PlanView> search(Long managerId, String month, String status, String keyword,
                                       int page, int size) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        if (managerId != null) {
            where.append(" AND p.manager_id = ?");
            args.add(managerId);
        }
        if (month != null && !month.isBlank()) {
            where.append(" AND p.plan_month = ?");
            args.add(month);
        }
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (g.name LIKE ? OR g.code LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
        }
        if ("TODO".equalsIgnoreCase(status)) {
            where.append(" AND v.id IS NULL");
        } else if ("DONE".equalsIgnoreCase(status)) {
            where.append(" AND v.id IS NOT NULL");
        }

        Long total = jdbc.queryForObject(COUNT + where, Long.class, args.toArray());
        List<Object> listArgs = new ArrayList<>(args);
        listArgs.add(size);
        listArgs.add((long) (page - 1) * size);
        List<PlanView> list = jdbc.query(SELECT + where + " ORDER BY p.plan_month DESC, g.code ASC LIMIT ? OFFSET ?",
                MAPPER, listArgs.toArray());
        return PageResult.of(list, total == null ? 0 : total, page, size);
    }

    public List<PlanView> findTodo(Long managerId, String month, int limit) {
        StringBuilder sql = new StringBuilder(SELECT).append(" WHERE v.id IS NULL");
        List<Object> args = new ArrayList<>();
        if (managerId != null) {
            sql.append(" AND p.manager_id = ?");
            args.add(managerId);
        }
        if (month != null && !month.isBlank()) {
            sql.append(" AND p.plan_month = ?");
            args.add(month);
        }
        sql.append(" ORDER BY p.plan_month ASC, g.code ASC LIMIT ?");
        args.add(limit);
        return jdbc.query(sql.toString(), MAPPER, args.toArray());
    }

    public List<PlanView> findAll(Long managerId, String month) {
        StringBuilder sql = new StringBuilder(SELECT).append(" WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        if (managerId != null) {
            sql.append(" AND p.manager_id = ?");
            args.add(managerId);
        }
        if (month != null && !month.isBlank()) {
            sql.append(" AND p.plan_month = ?");
            args.add(month);
        }
        sql.append(" ORDER BY u.real_name, p.plan_month, g.code");
        return jdbc.query(sql.toString(), MAPPER, args.toArray());
    }

    public Optional<PlanView> findViewById(long id) {
        List<PlanView> list = jdbc.query(SELECT + " WHERE p.id = ?", MAPPER, id);
        return list.stream().findFirst();
    }

    public Optional<PlanOwner> findOwner(long id) {
        List<PlanOwner> list = jdbc.query("SELECT id, manager_id, group_id, plan_month FROM visit_plan WHERE id = ?",
                (rs, i) -> new PlanOwner(rs.getLong("id"), rs.getLong("manager_id"),
                        rs.getLong("group_id"), rs.getString("plan_month")), id);
        return list.stream().findFirst();
    }

    public boolean exists(Long managerId, Long groupId, String month, Long excludeId) {
        Long n;
        if (excludeId == null) {
            n = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM visit_plan WHERE manager_id = ? AND group_id = ? AND plan_month = ?",
                    Long.class, managerId, groupId, month);
        } else {
            n = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM visit_plan WHERE manager_id = ? AND group_id = ? AND plan_month = ? AND id <> ?",
                    Long.class, managerId, groupId, month, excludeId);
        }
        return n != null && n > 0;
    }

    public long insert(Long managerId, Long groupId, String month, String remark) {
        KeyHolder holder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO visit_plan (manager_id, group_id, plan_month, remark) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, managerId);
            ps.setLong(2, groupId);
            ps.setString(3, month);
            ps.setString(4, remark);
            return ps;
        }, holder);
        Number key = holder.getKey();
        return key == null ? 0 : key.longValue();
    }

    public int update(long id, Long managerId, Long groupId, String month, String remark) {
        return jdbc.update("UPDATE visit_plan SET manager_id = ?, group_id = ?, plan_month = ?, remark = ? WHERE id = ?",
                managerId, groupId, month, remark, id);
    }

    public int delete(long id) {
        return jdbc.update("DELETE FROM visit_plan WHERE id = ?", id);
    }

    public int deleteByManagersAndMonths(List<Long> managerIds, List<String> months) {
        if (managerIds.isEmpty() || months.isEmpty()) {
            return 0;
        }
        String mgrIn = String.join(",", managerIds.stream().map(x -> "?").toList());
        String monIn = String.join(",", months.stream().map(x -> "?").toList());
        List<Object> args = new ArrayList<>(managerIds);
        args.addAll(months);
        return jdbc.update("DELETE FROM visit_plan WHERE manager_id IN (" + mgrIn + ") AND plan_month IN (" + monIn + ")",
                args.toArray());
    }

    public List<String> distinctMonths(Long managerId) {
        String sql = managerId == null
                ? "SELECT DISTINCT plan_month FROM visit_plan ORDER BY plan_month"
                : "SELECT DISTINCT plan_month FROM visit_plan WHERE manager_id = ? ORDER BY plan_month";
        return managerId == null
                ? jdbc.queryForList(sql, String.class)
                : jdbc.queryForList(sql, String.class, managerId);
    }
}
