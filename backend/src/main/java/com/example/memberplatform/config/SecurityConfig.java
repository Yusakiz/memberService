package com.example.memberplatform.config;

import java.io.IOException;
import java.util.Locale;

import com.example.memberplatform.dao.MemberDao;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
    UserDetailsService userDetailsService(MemberDao memberDao) {
        return rawUsername -> memberDao.findByUsername(rawUsername.trim().toLowerCase(Locale.ROOT))
                .map(member -> User.withUsername(member.username())
                        .password(member.passwordHash())
                        .roles("MEMBER")
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("會員不存在"));
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        HttpSessionCsrfTokenRepository csrfRepository = new HttpSessionCsrfTokenRepository();
        csrfRepository.setHeaderName("X-CSRF-TOKEN");

        http
                .csrf(csrf -> csrf.csrfTokenRepository(csrfRepository))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/auth/csrf", "/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .formLogin(form -> form
                        .loginProcessingUrl("/api/auth/login")
                        .successHandler((request, response, authentication) ->
                                writeJson(response, 200, "登入成功"))
                        .failureHandler((request, response, exception) ->
                                writeJson(response, 401, "帳號或密碼錯誤")))
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(HttpServletResponse.SC_NO_CONTENT)))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, error) ->
                                writeJson(response, 401, "請先登入"))
                        .accessDeniedHandler((request, response, error) ->
                                writeJson(response, 403, "安全驗證失敗，請重新整理後再試")))
                .requestCache(cache -> cache.disable());

        return http.build();
    }

    private void writeJson(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }
}
