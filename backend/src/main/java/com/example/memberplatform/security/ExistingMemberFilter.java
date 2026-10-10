package com.example.memberplatform.security;

import java.io.IOException;

import com.example.memberplatform.error.ApiError;
import com.example.memberplatform.member.MemberService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import tools.jackson.databind.ObjectMapper;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ExistingMemberFilter extends OncePerRequestFilter {

    private final MemberService memberService;
    private final JsonResponseWriter json;

    public ExistingMemberFilter(MemberService memberService, ObjectMapper objectMapper) {
        this.memberService = memberService;
        this.json = new JsonResponseWriter(objectMapper);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/members");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof MemberPrincipal principal
                && !memberService.exists(principal.getMemberId())) {
            SecurityContextHolder.clearContext();
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            json.write(response, HttpServletResponse.SC_UNAUTHORIZED, new ApiError("登入狀態已失效"));
            return;
        }
        filterChain.doFilter(request, response);
    }
}
