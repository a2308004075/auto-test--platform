package com.postman.platform.auth.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 配置 - 基于 JWT 的无状态认证
 *
 * <p>详细的 JwtAuthenticationFilter 和 JwtTokenProvider 将在 Phase 0 实现。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeRequests()
                // 公开端点
                .antMatchers(
                    "/api/v1/auth/login",
                    "/api/v1/auth/refresh",
                    "/actuator/health"
                ).permitAll()
                // 其余端点需要认证
                .anyRequest().authenticated();

        return http.build();
    }
}
