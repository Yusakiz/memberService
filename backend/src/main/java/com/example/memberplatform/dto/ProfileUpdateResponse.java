package com.example.memberplatform.dto;

public record ProfileUpdateResponse(MemberResponse member, boolean reLoginRequired) {
}
