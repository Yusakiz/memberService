package com.example.memberplatform.member;

import java.sql.SQLException;
import java.util.Locale;
import java.util.List;
import java.util.Map;

import com.example.memberplatform.dto.ChangePasswordRequest;
import com.example.memberplatform.dto.MemberResponse;
import com.example.memberplatform.dto.MemberSearchResponse;
import com.example.memberplatform.dto.RegisterRequest;
import com.example.memberplatform.dto.UpdateProfileRequest;
import com.example.memberplatform.error.ApiException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberService {

    private static final int PAGE_SIZE = 20;

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public MemberService(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Member register(RegisterRequest request) {
        String username = normalizeUsername(request.username());
        validateUsername(username);
        String nickname = validateNickname(request.nickname());
        if (!request.password().equals(request.confirmPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "輸入資料有誤",
                    Map.of("confirmPassword", "兩次輸入的密碼不一致"));
        }
        if (memberRepository.findByUsername(username).isPresent()) {
            throw usernameConflict();
        }

        try {
            return memberRepository.create(username, nickname, passwordEncoder.encode(request.password()));
        } catch (DataIntegrityViolationException exception) {
            if (isUsernameUniqueViolation(exception)) {
                throw usernameConflict();
            }
            throw exception;
        }
    }

    public Member getRequired(long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "登入狀態已失效"));
    }

    public boolean exists(long memberId) {
        return memberRepository.findById(memberId).isPresent();
    }

    public MemberSearchResponse search(String field, String rawKeyword, int page) {
        if (!"username".equals(field) && !"nickname".equals(field)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "查詢類型必須是帳號或暱稱");
        }
        if (page < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "頁碼不可小於 0");
        }
        String keyword = rawKeyword == null ? "" : rawKeyword.trim();
        int keywordLength = keyword.codePointCount(0, keyword.length());
        if (keywordLength < 1 || keywordLength > 30) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "查詢關鍵字須為 1～30 個字");
        }

        if ("username".equals(field)) {
            keyword = keyword.toLowerCase(Locale.ROOT);
        }
        String escapedKeyword = escapeLike(keyword);
        long offset = (long) page * PAGE_SIZE;
        List<Member> rows = "username".equals(field)
                ? memberRepository.searchByUsername(escapedKeyword, offset, PAGE_SIZE + 1)
                : memberRepository.searchByNickname(escapedKeyword, offset, PAGE_SIZE + 1);
        boolean hasNext = rows.size() > PAGE_SIZE;
        List<MemberResponse> items = rows.stream()
                .limit(PAGE_SIZE)
                .map(MemberResponse::from)
                .toList();
        return new MemberSearchResponse(items, page, hasNext);
    }

    @Transactional
    public ProfileUpdate updateProfile(long memberId, UpdateProfileRequest request) {
        Member existing = getRequired(memberId);
        String username = normalizeUsername(request.username());
        validateUsername(username);
        String nickname = validateNickname(request.nickname());
        if (!username.equals(existing.username())
                && memberRepository.existsByUsernameExcludingId(username, memberId)) {
            throw usernameConflict();
        }

        try {
            memberRepository.updateProfile(memberId, username, nickname);
        } catch (DataIntegrityViolationException exception) {
            if (isUsernameUniqueViolation(exception)) {
                throw usernameConflict();
            }
            throw exception;
        }
        return new ProfileUpdate(new Member(memberId, username, nickname, existing.passwordHash()),
                !username.equals(existing.username()));
    }

    @Transactional
    public void changePassword(long memberId, ChangePasswordRequest request) {
        Member existing = getRequired(memberId);
        if (!passwordEncoder.matches(request.currentPassword(), existing.passwordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "輸入資料有誤",
                    Map.of("currentPassword", "目前密碼錯誤"));
        }
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "輸入資料有誤",
                    Map.of("confirmPassword", "兩次輸入的新密碼不一致"));
        }
        memberRepository.updatePassword(memberId, passwordEncoder.encode(request.newPassword()));
    }

    @Transactional
    public void delete(long memberId) {
        getRequired(memberId);
        memberRepository.delete(memberId);
    }

    public static String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    private void validateUsername(String username) {
        if (!username.matches("[a-z0-9_]{4,20}")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "輸入資料有誤",
                    Map.of("username", "帳號限 4～20 個半形英文字母、數字或底線"));
        }
    }

    private String validateNickname(String rawNickname) {
        String nickname = rawNickname.trim();
        int length = nickname.codePointCount(0, nickname.length());
        boolean hasControlCharacter = nickname.codePoints().anyMatch(Character::isISOControl);
        if (length < 1 || length > 30 || hasControlCharacter) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "輸入資料有誤",
                    Map.of("nickname", "暱稱去除頭尾空白後須為 1～30 個字，且不可包含換行或控制字元"));
        }
        return nickname;
    }

    private ApiException usernameConflict() {
        return new ApiException(HttpStatus.CONFLICT, "帳號已被使用",
                Map.of("username", "帳號已被使用"));
    }

    private boolean isUsernameUniqueViolation(DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof SQLException sqlException
                    && sqlException.getErrorCode() == 19
                    && sqlException.getMessage() != null
                    && sqlException.getMessage().contains("members.username")) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private String escapeLike(String value) {
        return value.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    public record ProfileUpdate(Member member, boolean reLoginRequired) {
    }
}
