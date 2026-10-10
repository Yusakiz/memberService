package com.example.memberplatform.service;

import java.util.List;
import java.util.Locale;

import com.example.memberplatform.dao.MemberDao;
import com.example.memberplatform.model.Member;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberService {

    private final MemberDao memberDao;
    private final PasswordEncoder passwordEncoder;

    public MemberService(MemberDao memberDao, PasswordEncoder passwordEncoder) {
        this.memberDao = memberDao;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Member register(String rawUsername, String rawName, String password, String confirmPassword) {
        String username = normalizeUsername(rawUsername);
        String name = validateName(rawName);
        validateUsername(username);
        validatePassword(password);
        if (!password.equals(confirmPassword)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "兩次輸入的密碼不一致");
        }
        if (memberDao.findByUsername(username).isPresent()) {
            throw new BusinessException(HttpStatus.CONFLICT, "帳號已被使用");
        }
        try {
            memberDao.insert(username, name, passwordEncoder.encode(password));
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(HttpStatus.CONFLICT, "帳號已被使用");
        }
        return getByUsername(username);
    }

    public Member getByUsername(String rawUsername) {
        return memberDao.findByUsername(normalizeUsername(rawUsername))
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "登入狀態已失效"));
    }

    public List<Member> search(String field, String rawKeyword) {
        if (!"username".equals(field) && !"name".equals(field)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "查詢類型錯誤");
        }
        String keyword = rawKeyword == null ? "" : rawKeyword.trim();
        if (keyword.isEmpty() || keyword.length() > 30) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "查詢關鍵字須為 1～30 個字");
        }
        if ("username".equals(field)) {
            keyword = keyword.toLowerCase(Locale.ROOT);
        }
        return memberDao.search(field, keyword);
    }

    @Transactional
    public UpdateResult update(String currentUsername, String rawUsername, String rawName) {
        Member current = getByUsername(currentUsername);
        String username = normalizeUsername(rawUsername);
        String name = validateName(rawName);
        validateUsername(username);
        if (!username.equals(current.username())
                && memberDao.usernameExistsForAnotherMember(username, current.id())) {
            throw new BusinessException(HttpStatus.CONFLICT, "帳號已被使用");
        }
        memberDao.update(current.id(), username, name);
        return new UpdateResult(getByUsername(username), !username.equals(current.username()));
    }

    @Transactional
    public void delete(String currentUsername) {
        Member current = getByUsername(currentUsername);
        memberDao.delete(current.id());
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    private void validateUsername(String username) {
        if (!username.matches("[a-z0-9_]{4,20}")) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "帳號限 4～20 個半形英文字母、數字或底線");
        }
    }

    private String validateName(String rawName) {
        String name = rawName == null ? "" : rawName.trim();
        if (name.isEmpty() || name.codePointCount(0, name.length()) > 30
                || name.codePoints().anyMatch(Character::isISOControl)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "姓名須為 1～30 個字，且不可包含控制字元");
        }
        return name;
    }

    private void validatePassword(String password) {
        if (password == null || !password.matches("[!-~]{8,64}")) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "密碼限 8～64 個半形英數或符號，不含空白");
        }
    }

    public record UpdateResult(Member member, boolean usernameChanged) {
    }

    public static class BusinessException extends RuntimeException {
        private final HttpStatus status;

        public BusinessException(HttpStatus status, String message) {
            super(message);
            this.status = status;
        }

        public HttpStatus getStatus() {
            return status;
        }
    }
}
