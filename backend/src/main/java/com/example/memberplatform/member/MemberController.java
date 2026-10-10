package com.example.memberplatform.member;

import com.example.memberplatform.dto.MemberResponse;
import com.example.memberplatform.dto.MemberSearchResponse;
import com.example.memberplatform.dto.ChangePasswordRequest;
import com.example.memberplatform.dto.ProfileUpdateResponse;
import com.example.memberplatform.dto.UpdateProfileRequest;
import com.example.memberplatform.security.MemberPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping
    MemberSearchResponse search(
            @RequestParam String field,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page) {
        return memberService.search(field, keyword, page);
    }

    @PutMapping("/me")
    ProfileUpdateResponse updateProfile(
            @AuthenticationPrincipal MemberPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request,
            HttpServletRequest servletRequest) {
        MemberService.ProfileUpdate result = memberService.updateProfile(principal.getMemberId(), request);
        if (result.reLoginRequired()) {
            invalidateSession(servletRequest);
        }
        return new ProfileUpdateResponse(MemberResponse.from(result.member()), result.reLoginRequired());
    }

    @PutMapping("/me/password")
    ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal MemberPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest servletRequest) {
        memberService.changePassword(principal.getMemberId(), request);
        invalidateSession(servletRequest);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    ResponseEntity<Void> delete(
            @AuthenticationPrincipal MemberPrincipal principal,
            HttpServletRequest servletRequest) {
        memberService.delete(principal.getMemberId());
        invalidateSession(servletRequest);
        return ResponseEntity.noContent().build();
    }

    private void invalidateSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
