package com.example.memberplatform.dao;

import java.util.List;
import java.util.Optional;

import com.example.memberplatform.model.Member;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class MemberDao {

    private static final RowMapper<Member> ROW_MAPPER = (resultSet, rowNumber) -> new Member(
            resultSet.getLong("id"),
            resultSet.getString("username"),
            resultSet.getString("name"),
            resultSet.getString("password_hash"));

    private final JdbcTemplate jdbcTemplate;

    public MemberDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Member> findByUsername(String username) {
        List<Member> members = jdbcTemplate.query(
                "SELECT id, username, name, password_hash FROM members WHERE username = ?",
                ROW_MAPPER,
                username);
        return members.stream().findFirst();
    }

    public void insert(String username, String name, String passwordHash) {
        jdbcTemplate.update(
                "INSERT INTO members (username, name, password_hash) VALUES (?, ?, ?)",
                username,
                name,
                passwordHash);
    }

    public List<Member> search(String field, String keyword) {
        String sql;
        if ("username".equals(field)) {
            sql = "SELECT id, username, name, password_hash FROM members "
                    + "WHERE username LIKE ? ESCAPE '\\' ORDER BY username LIMIT 50";
        } else if ("name".equals(field)) {
            sql = "SELECT id, username, name, password_hash FROM members "
                    + "WHERE name LIKE ? ESCAPE '\\' COLLATE NOCASE ORDER BY username LIMIT 50";
        } else {
            throw new IllegalArgumentException("不支援的查詢欄位");
        }
        return jdbcTemplate.query(sql, ROW_MAPPER, "%" + escapeLike(keyword) + "%");
    }

    public boolean usernameExistsForAnotherMember(String username, long memberId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM members WHERE username = ? AND id <> ?",
                Integer.class,
                username,
                memberId);
        return count != null && count > 0;
    }

    public void update(long id, String username, String name) {
        jdbcTemplate.update("UPDATE members SET username = ?, name = ? WHERE id = ?", username, name, id);
    }

    public void delete(long id) {
        jdbcTemplate.update("DELETE FROM members WHERE id = ?", id);
    }

    private String escapeLike(String value) {
        return value.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
