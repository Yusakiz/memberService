package com.example.memberplatform.config;

import java.io.IOException;

import com.example.memberplatform.dto.MemberResponse;
import com.example.memberplatform.error.ApiError;
import com.example.memberplatform.member.MemberService;
import com.example.memberplatform.security.JsonResponseWriter;
import com.example.memberplatform.security.MemberPrincipal;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    HttpSessionCsrfTokenRepository csrfTokenRepository() {
        HttpSessionCsrfTokenRepository repository = new HttpSessionCsrfTokenRepository();
        repository.setHeaderName("X-CSRF-TOKEN");
        return repository;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            HttpSessionCsrfTokenRepository csrfTokenRepository,
            ObjectMapper objectMapper,
            MemberService memberService) throws Exception {
        JsonResponseWriter json = new JsonResponseWriter(objectMapper);

        http
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokenRepository))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/auth/csrf", "/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .formLogin(form -> form
                        .loginProcessingUrl("/api/auth/login")
                        .successHandler((request, response, authentication) -> {
                            MemberPrincipal principal = (MemberPrincipal) authentication.getPrincipal();
                            json.write(response, HttpServletResponse.SC_OK,
                                    MemberResponse.from(memberService.getRequired(principal.getMemberId())));
                        })
                        .failureHandler((request, response, exception) -> json.write(
                                response,
                                HttpServletResponse.SC_UNAUTHORIZED,
                                new ApiError("帳號或密碼錯誤"))))
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(HttpServletResponse.SC_NO_CONTENT)))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> json.write(
                                response,
                                HttpServletResponse.SC_UNAUTHORIZED,
                                new ApiError("請先登入")))
                        .accessDeniedHandler((request, response, exception) -> json.write(
                                response,
                                HttpStatus.FORBIDDEN.value(),
                                new ApiError("CSRF 驗證失敗，請重新整理後再試"))))
                .requestCache(cache -> cache.disable());

        return http.build();
    }
}
