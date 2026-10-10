package com.example.memberplatform.member;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class MemberRepository {

    private static final RowMapper<Member> MEMBER_ROW_MAPPER = (resultSet, rowNumber) -> new Member(
            resultSet.getLong("id"),
            resultSet.getString("username"),
            resultSet.getString("nickname"),
            resultSet.getString("password_hash"));

    private final JdbcTemplate jdbcTemplate;

    public MemberRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Member> findById(long id) {
        List<Member> members = jdbcTemplate.query(
                "SELECT id, username, nickname, password_hash FROM members WHERE id = ?",
                MEMBER_ROW_MAPPER,
                id);
        return members.stream().findFirst();
    }

    public Optional<Member> findByUsername(String username) {
        List<Member> members = jdbcTemplate.query(
                "SELECT id, username, nickname, password_hash FROM members WHERE username = ?",
                MEMBER_ROW_MAPPER,
                username);
        return members.stream().findFirst();
    }

    public Member create(String username, String nickname, String passwordHash) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO members (username, nickname, password_hash) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, username);
            statement.setString(2, nickname);
            statement.setString(3, passwordHash);
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("新增會員後未取得識別碼");
        }
        return new Member(key.longValue(), username, nickname, passwordHash);
    }
}
