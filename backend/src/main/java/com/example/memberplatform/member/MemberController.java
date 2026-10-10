package com.example.memberplatform.member;

import com.example.memberplatform.dto.MemberResponse;
import com.example.memberplatform.security.MemberPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/me")
    MemberResponse me(@AuthenticationPrincipal MemberPrincipal principal) {
        return MemberResponse.from(memberService.getRequired(principal.getMemberId()));
    }
}
