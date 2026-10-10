package com.example.memberplatform.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateProfileRequest(
        @NotNull(message = "請輸入帳號") String username,
        @NotNull(message = "請輸入暱稱") String nickname) {
}
