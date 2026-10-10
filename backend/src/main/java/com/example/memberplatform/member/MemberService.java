package com.example.memberplatform.member;

import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;

import com.example.memberplatform.dto.RegisterRequest;
import com.example.memberplatform.error.ApiException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public MemberService(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Member register(RegisterRequest request) {
        String username = normalizeUsername(request.username());
        if (!username.matches("[a-z0-9_]{4,20}")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "輸入資料有誤",
                    Map.of("username", "帳號限 4～20 個半形英文字母、數字或底線"));
        }
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

    public static String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
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
}
