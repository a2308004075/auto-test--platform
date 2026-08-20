package com.postman.platform.auth.security;

import com.postman.platform.auth.entity.User;
import com.postman.platform.auth.mapper.TokenBlacklistMapper;
import com.postman.platform.auth.mapper.UserMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

/**
 * JWT 认证过滤器 - 从请求头解析 Token 并设置 SecurityContext
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;
    private final TokenBlacklistMapper tokenBlacklistMapper;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   UserMapper userMapper,
                                   TokenBlacklistMapper tokenBlacklistMapper) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userMapper = userMapper;
        this.tokenBlacklistMapper = tokenBlacklistMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null) {
            try {
                Claims claims = jwtTokenProvider.parseToken(token);
                if (jwtTokenProvider.isAccessToken(claims)) {
                    String jti = jwtTokenProvider.getJti(claims);
                    // 检查 Token 是否在黑名单中
                    if (tokenBlacklistMapper.existsByTokenJti(jti)) {
                        log.debug("Token 已在黑名单中, jti={}", jti);
                    } else {
                        String userId = jwtTokenProvider.getUserId(claims);
                        User user = userMapper.selectActiveById(userId);
                        if (user != null) {
                            // 权限信息从 JWT claims 中获取（登录时已写入 role_code）
                            String role = jwtTokenProvider.getRole(claims);
                            UsernamePasswordAuthenticationToken auth =
                                    new UsernamePasswordAuthenticationToken(
                                            user,
                                            null,
                                            Collections.singletonList(
                                                    new SimpleGrantedAuthority("ROLE_" + role))
                                    );
                            SecurityContextHolder.getContext().setAuthentication(auth);
                        }
                    }
                }
            } catch (ExpiredJwtException e) {
                log.debug("Token 已过期: {}", e.getMessage());
            } catch (JwtException e) {
                log.debug("Token 无效: {}", e.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }

    /**
     * 从 Authorization 请求头提取 Bearer Token
     */
    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader(AUTHORIZATION_HEADER);
        if (bearer != null && bearer.startsWith(BEARER_PREFIX)) {
            return bearer.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
