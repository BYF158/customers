package com.example.visit.repo;

import com.example.visit.common.PageResult;
import com.example.visit.dto.Views.VisitView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class VisitRepository {

    private final JdbcTemplate jdbc;

    public VisitRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String SELECT = """
            SELECT v.id, v.plan_id, p.manager_id, u.real_name AS manager_name,
                   g.code AS group_code, g.name AS group_name, p.plan_month,
                   v.visit_date, v.method, v.result, v.remark, v.created_at
            FROM visit_record v
            JOIN visit_plan p ON p.id = v.plan_id
            JOIN sys_user u ON u.id = p.manager_id
            JOIN customer_group g ON g.id = p.group_id
            """;

    private static final RowMapper<VisitView> MAPPER = (rs, i) -> new VisitView(
            rs.getLong("id"),
            rs.getLong("plan_id"),
            rs.getLong("manager_id"),
            rs.getString("manager_name"),
            rs.getString("group_code"),
            rs.getString("group_name"),
            rs.getString("plan_month"),
            rs.getDate("visit_date") == null ? null : rs.getDate("visit_date").toLocalDate(),
            rs.getString("method"),
            rs.getString("result"),
            rs.getString("remark"),
            UserRepository.toTime(rs, "created_at"));

    public boolean existsByPlan(long planId) {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM visit_record WHERE plan_id = ?", Long.class, planId);
        return n != null && n > 0;
    }

    public Optional<VisitView> findByPlan(long planId) {
        List<VisitView> list = jdbc.query(SELECT + " WHERE v.plan_id = ?", MAPPER, planId);
        return list.stream().findFirst();
    }

    public Optional<VisitView> findById(long id) {
        List<VisitView> list = jdbc.query(SELECT + " WHERE v.id = ?", MAPPER, id);
        return list.stream().findFirst();
    }

    public PageResult<VisitView> search(Long managerId, String month, String keyword, int page, int size) {
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
        String countSql = """
                SELECT COUNT(*) FROM visit_record v
                JOIN visit_plan p ON p.id = v.plan_id
                JOIN customer_group g ON g.id = p.group_id
                """ + where;
        Long total = jdbc.queryForObject(countSql, Long.class, args.toArray());
        List<Object> listArgs = new ArrayList<>(args);
        listArgs.add(size);
        listArgs.add((long) (page - 1) * size);
        List<VisitView> list = jdbc.query(SELECT + where + " ORDER BY v.visit_date DESC, v.id DESC LIMIT ? OFFSET ?",
                MAPPER, listArgs.toArray());
        return PageResult.of(list, total == null ? 0 : total, page, size);
    }

    public List<VisitView> findAll(Long managerId, String month) {
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

    public long insert(long planId, LocalDate date, String method, String result, String remark, Long createdBy) {
        org.springframework.jdbc.support.KeyHolder holder = new org.springframework.jdbc.support.GeneratedKeyHolder();
        jdbc.update(con -> {
            java.sql.PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO visit_record (plan_id, visit_date, method, result, remark, created_by) VALUES (?, ?, ?, ?, ?, ?)",
                    java.sql.Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, planId);
            ps.setDate(2, java.sql.Date.valueOf(date));
            ps.setString(3, method);
            ps.setString(4, result);
            ps.setString(5, remark);
            if (createdBy == null) {
                ps.setNull(6, java.sql.Types.BIGINT);
            } else {
                ps.setLong(6, createdBy);
            }
            return ps;
        }, holder);
        Number key = holder.getKey();
        return key == null ? 0 : key.longValue();
    }

    public int update(long id, LocalDate date, String method, String result, String remark) {
        return jdbc.update("UPDATE visit_record SET visit_date = ?, method = ?, result = ?, remark = ? WHERE id = ?",
                java.sql.Date.valueOf(date), method, result, remark, id);
    }

    public int deleteByPlan(long planId) {
        return jdbc.update("DELETE FROM visit_record WHERE plan_id = ?", planId);
    }

    public int delete(long id) {
        return jdbc.update("DELETE FROM visit_record WHERE id = ?", id);
    }
}
