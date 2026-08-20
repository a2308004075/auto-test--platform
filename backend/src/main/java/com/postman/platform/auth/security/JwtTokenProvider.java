package com.postman.platform.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

/**
 * JWT Token 签发与验证
 */
@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-expire-ms}")
    private long accessTokenExpireMs;

    @Value("${jwt.refresh-token-expire-ms}")
    private long refreshTokenExpireMs;

    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";
    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_ROLE = "role";

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 创建 Access Token
     *
     * @param userId 用户 ID
     * @param role   用户角色
     * @return JWT Access Token
     */
    public String createAccessToken(Long userId, String role) {
        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(String.valueOf(userId))
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_TYPE, TOKEN_TYPE_ACCESS)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpireMs))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 创建 Refresh Token
     *
     * @param userId 用户 ID
     * @return JWT Refresh Token
     */
    public String createRefreshToken(Long userId) {
        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(String.valueOf(userId))
                .claim(CLAIM_TYPE, TOKEN_TYPE_REFRESH)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshTokenExpireMs))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 解析 Token，验证签名和有效期
     *
     * @param token JWT 字符串
     * @return Claims
     * @throws ExpiredJwtException Token 已过期
     * @throws JwtException        Token 无效
     */
    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 尝试解析 Token，不抛异常
     *
     * @param token JWT 字符串
     * @return Claims 或 null
     */
    public Claims tryParseToken(String token) {
        try {
            return parseToken(token);
        } catch (JwtException e) {
            log.debug("Token 解析失败: {}", e.getMessage());
            return null;
        }
    }

    public String getUserId(Claims claims) {
        return claims.getSubject();
    }

    public String getTokenType(Claims claims) {
        return claims.get(CLAIM_TYPE, String.class);
    }

    public String getRole(Claims claims) {
        return claims.get(CLAIM_ROLE, String.class);
    }

    public String getJti(Claims claims) {
        return claims.getId();
    }

    public Date getExpiration(Claims claims) {
        return claims.getExpiration();
    }

    public long getAccessTokenExpireMs() {
        return accessTokenExpireMs;
    }

    /**
     * 判断 Token 是否为 Access Token
     */
    public boolean isAccessToken(Claims claims) {
        return TOKEN_TYPE_ACCESS.equals(getTokenType(claims));
    }

    /**
     * 判断 Token 是否为 Refresh Token
     */
    public boolean isRefreshToken(Claims claims) {
        return TOKEN_TYPE_REFRESH.equals(getTokenType(claims));
    }
}
