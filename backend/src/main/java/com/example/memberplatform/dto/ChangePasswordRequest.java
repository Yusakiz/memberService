package com.example.memberplatform.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ChangePasswordRequest(
        @NotNull(message = "請輸入目前密碼")
        String currentPassword,
        @NotNull(message = "請輸入新密碼")
        @Pattern(regexp = "[!-~]{8,64}", message = "密碼限 8～64 個半形英數或符號，不含空白")
        String newPassword,
        @NotNull(message = "請再次輸入新密碼")
        String confirmPassword) {
}
