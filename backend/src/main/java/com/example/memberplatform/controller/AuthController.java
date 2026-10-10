package com.example.memberplatform.controller;

import com.example.memberplatform.model.Member;
import com.example.memberplatform.service.MemberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final MemberService memberService;

    public AuthController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/csrf")
    CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getToken(), token.getHeaderName());
    }

    @PostMapping("/register")
    ResponseEntity<MemberResponse> register(@RequestBody RegisterRequest request) {
        Member member = memberService.register(
                request.username(), request.name(), request.password(), request.confirmPassword());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new MemberResponse(member.username(), member.name()));
    }

    public record RegisterRequest(String username, String name, String password, String confirmPassword) {
    }

    public record MemberResponse(String username, String name) {
    }

    public record CsrfResponse(String token, String headerName) {
    }
}
