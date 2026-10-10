package com.example.memberplatform.dto;

import com.example.memberplatform.member.Member;

public record MemberResponse(String username, String nickname) {

    public static MemberResponse from(Member member) {
        return new MemberResponse(member.username(), member.nickname());
    }
}
