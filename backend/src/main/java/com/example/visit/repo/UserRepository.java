package com.example.visit.repo;

import com.example.visit.dto.Views.ManagerView;
import com.example.visit.entity.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<User> USER_MAPPER = (rs, i) -> new User(
            rs.getLong("id"),
            rs.getString("username"),
            rs.getString("real_name"),
            rs.getString("password"),
            rs.getString("role"),
            rs.getBoolean("enabled"),
            toTime(rs, "created_at"),
            toTime(rs, "updated_at"));

    static java.time.LocalDateTime toTime(ResultSet rs, String column) throws SQLException {
        Timestamp ts = rs.getTimestamp(column);
        return ts == null ? null : ts.toLocalDateTime();
    }

    public Optional<User> findByUsername(String username) {
        List<User> list = jdbc.query("SELECT * FROM sys_user WHERE username = ?", USER_MAPPER, username);
        return list.stream().findFirst();
    }

    public Optional<User> findById(long id) {
        List<User> list = jdbc.query("SELECT * FROM sys_user WHERE id = ?", USER_MAPPER, id);
        return list.stream().findFirst();
    }

    public Optional<User> findManagerByName(String name) {
        List<User> list = jdbc.query(
                "SELECT * FROM sys_user WHERE role = 'MANAGER' AND (real_name = ? OR username = ?) ORDER BY id LIMIT 1",
                USER_MAPPER, name, name);
        return list.stream().findFirst();
    }

    public List<ManagerView> findManagers() {
        String sql = """
                SELECT u.id, u.username, u.real_name, u.role, u.enabled, u.created_at,
                       (SELECT COUNT(*) FROM visit_plan p WHERE p.manager_id = u.id) AS plan_count,
                       (SELECT COUNT(*) FROM visit_record v WHERE v.created_by = u.id OR v.plan_id IN
                            (SELECT p2.id FROM visit_plan p2 WHERE p2.manager_id = u.id)) AS visit_count
                FROM sys_user u
                WHERE u.role = 'MANAGER'
                ORDER BY u.id
                """;
        return jdbc.query(sql, (rs, i) -> new ManagerView(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("real_name"),
                rs.getString("role"),
                rs.getBoolean("enabled"),
                rs.getLong("plan_count"),
                rs.getLong("visit_count"),
                toTime(rs, "created_at")));
    }

    public long countByRole(String role) {
        Long n = jdbc.queryForObject("SELECT COUNT(*) FROM sys_user WHERE role = ?", Long.class, role);
        return n == null ? 0 : n;
    }

    public long insert(String username, String realName, String password, String role, boolean enabled) {
        String sql = "INSERT INTO sys_user (username, real_name, password, role, enabled) VALUES (?, ?, ?, ?, ?)";
        KeyHolder holder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, username);
            ps.setString(2, realName);
            ps.setString(3, password);
            ps.setString(4, role);
            ps.setBoolean(5, enabled);
            return ps;
        }, holder);
        Number key = holder.getKey();
        return key == null ? 0 : key.longValue();
    }

    public int updateBasic(long id, String realName, boolean enabled) {
        return jdbc.update("UPDATE sys_user SET real_name = ?, enabled = ? WHERE id = ?", realName, enabled, id);
    }

    public int updatePassword(long id, String password) {
        return jdbc.update("UPDATE sys_user SET password = ? WHERE id = ?", password, id);
    }

    public int delete(long id) {
        return jdbc.update("DELETE FROM sys_user WHERE id = ?", id);
    }
}
