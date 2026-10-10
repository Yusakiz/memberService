package com.example.memberplatform.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record RegisterRequest(
        @NotNull(message = "請輸入帳號")
        String username,
        @NotNull(message = "請輸入暱稱")
        String nickname,
        @NotNull(message = "請輸入密碼")
        @Pattern(regexp = "[!-~]{8,64}", message = "密碼限 8～64 個半形英數或符號，不含空白")
        String password,
        @NotNull(message = "請再次輸入密碼")
        String confirmPassword) {
}
