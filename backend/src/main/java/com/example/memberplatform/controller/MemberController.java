package com.example.memberplatform.controller;

import java.security.Principal;
import java.util.List;

import com.example.memberplatform.model.Member;
import com.example.memberplatform.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
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
    MemberResponse me(Principal principal) {
        return toResponse(memberService.getByUsername(principal.getName()));
    }

    @GetMapping
    List<MemberResponse> search(@RequestParam String field, @RequestParam String keyword) {
        return memberService.search(field, keyword).stream().map(this::toResponse).toList();
    }

    @PutMapping("/me")
    UpdateResponse update(
            Principal principal,
            @RequestBody UpdateRequest request,
            HttpServletRequest servletRequest) {
        MemberService.UpdateResult result = memberService.update(
                principal.getName(), request.username(), request.name());
        if (result.usernameChanged()) {
            invalidateSession(servletRequest);
        }
        return new UpdateResponse(toResponse(result.member()), result.usernameChanged());
    }

    @DeleteMapping("/me")
    ResponseEntity<Void> delete(Principal principal, HttpServletRequest request) {
        memberService.delete(principal.getName());
        invalidateSession(request);
        return ResponseEntity.noContent().build();
    }

    private MemberResponse toResponse(Member member) {
        return new MemberResponse(member.username(), member.name());
    }

    private void invalidateSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    public record UpdateRequest(String username, String name) {
    }

    public record MemberResponse(String username, String name) {
    }

    public record UpdateResponse(MemberResponse member, boolean reLoginRequired) {
    }
}
