package com.example.visit.repo;

import com.example.visit.dto.Views.GroupView;
import com.example.visit.entity.CustomerGroup;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class GroupRepository {

    private final JdbcTemplate jdbc;

    public GroupRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<CustomerGroup> MAPPER = (rs, i) -> new CustomerGroup(
            rs.getLong("id"),
            rs.getString("code"),
            rs.getString("name"),
            UserRepository.toTime(rs, "created_at"),
            UserRepository.toTime(rs, "updated_at"));

    public List<GroupView> findAll(String keyword) {
        StringBuilder sql = new StringBuilder("""
                SELECT g.id, g.code, g.name, g.created_at,
                       (SELECT COUNT(*) FROM visit_plan p WHERE p.group_id = g.id) AS plan_count
                FROM customer_group g
                """);
        List<Object> args = new ArrayList<>();
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" WHERE g.code LIKE ? OR g.name LIKE ?");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
        }
        sql.append(" ORDER BY g.code");
        return jdbc.query(sql.toString(), (rs, i) -> new GroupView(
                rs.getLong("id"),
                rs.getString("code"),
                rs.getString("name"),
                rs.getLong("plan_count"),
                UserRepository.toTime(rs, "created_at")), args.toArray());
    }

    public Optional<CustomerGroup> findById(long id) {
        List<CustomerGroup> list = jdbc.query("SELECT * FROM customer_group WHERE id = ?", MAPPER, id);
        return list.stream().findFirst();
    }

    public Optional<CustomerGroup> findByCode(String code) {
        List<CustomerGroup> list = jdbc.query("SELECT * FROM customer_group WHERE code = ?", MAPPER, code);
        return list.stream().findFirst();
    }

    public boolean existsCode(String code, Long excludeId) {
        Long n = excludeId == null
                ? jdbc.queryForObject("SELECT COUNT(*) FROM customer_group WHERE code = ?", Long.class, code)
                : jdbc.queryForObject("SELECT COUNT(*) FROM customer_group WHERE code = ? AND id <> ?",
                Long.class, code, excludeId);
        return n != null && n > 0;
    }

    public long insert(String code, String name) {
        KeyHolder holder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO customer_group (code, name) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, code);
            ps.setString(2, name);
            return ps;
        }, holder);
        Number key = holder.getKey();
        return key == null ? 0 : key.longValue();
    }

    public int update(long id, String code, String name) {
        return jdbc.update("UPDATE customer_group SET code = ?, name = ? WHERE id = ?", code, name, id);
    }

    public int rename(long id, String name) {
        return jdbc.update("UPDATE customer_group SET name = ? WHERE id = ?", name, id);
    }

    public int delete(long id) {
        return jdbc.update("DELETE FROM customer_group WHERE id = ?", id);
    }

    public long count() {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM customer_group", Long.class);
        return n == null ? 0 : n;
    }
}
