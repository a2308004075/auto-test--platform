package com.postman.platform.auth.service;

import com.postman.platform.auth.dto.*;
import com.postman.platform.auth.entity.TokenBlacklist;
import com.postman.platform.auth.entity.User;
import com.postman.platform.auth.entity.UserRole;
import com.postman.platform.auth.mapper.TokenBlacklistMapper;
import com.postman.platform.auth.mapper.UserMapper;
import com.postman.platform.auth.mapper.UserRoleMapper;
import com.postman.platform.auth.security.JwtTokenProvider;
import com.postman.platform.common.exception.BusinessException;
import com.postman.platform.common.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * 认证服务 - 登录、Token 刷新、登出
 */
@Slf4j
@Service
public class AuthService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final TokenBlacklistMapper tokenBlacklistMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final CaptchaService captchaService;

    public AuthService(UserMapper userMapper,
                       UserRoleMapper userRoleMapper,
                       TokenBlacklistMapper tokenBlacklistMapper,
                       JwtTokenProvider jwtTokenProvider,
                       PasswordEncoder passwordEncoder,
                       CaptchaService captchaService) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.tokenBlacklistMapper = tokenBlacklistMapper;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.captchaService = captchaService;
    }

    /**
     * 根据 roleId 查询角色编码
     *
     * @param roleId 角色 ID
     * @return 角色编码（如 ADMIN、USER），找不到时返回 null
     */
    private String getRoleCode(String roleId) {
        if (roleId == null) {
            return null;
        }
        UserRole role = userRoleMapper.selectById(roleId);
        return role != null ? role.getRoleCode() : null;
    }

    /**
     * 用户登录
     *
     * <p>流程：验证验证码 → 查询用户 → 检查启用状态 → bcrypt 验证密码 → 生成双 Token → 更新 lastLoginAt
     */
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse login(LoginRequest request) {
        // 验证码校验
        if (!captchaService.verifyCaptcha(request.getCaptchaId(), request.getCaptchaCode())) {
            throw new BusinessException(ErrorCode.CAPTCHA_INVALID, "验证码错误或已过期");
        }

        User user = userMapper.selectByUsername(request.getUsername());
        if (user == null) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED, "用户名或密码错误");
        }
        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new BusinessException(ErrorCode.ACCOUNT_RESERVED, "账号已被禁用");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED, "用户名或密码错误");
        }

        String roleCode = getRoleCode(user.getRoleId());
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), roleCode);
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        // 更新最后登录时间
        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);

        LoginResponse response = new LoginResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setExpiresIn(jwtTokenProvider.getAccessTokenExpireMs() / 1000);
        response.setUser(toUserBrief(user));

        log.info("用户登录成功: username={}", user.getUsername());
        return response;
    }

    /**
     * 刷新 Access Token
     *
     * <p>流程：解码 Refresh Token → 验证类型 → 检查黑名单 → 验证用户 → 生成新 Access Token
     */
    public TokenResponse refreshToken(RefreshRequest request) {
        Claims claims;
        try {
            claims = jwtTokenProvider.parseToken(request.getRefreshToken());
        } catch (ExpiredJwtException e) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_EXPIRED, "Refresh Token 已过期");
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "无效的 Refresh Token");
        }

        if (!jwtTokenProvider.isRefreshToken(claims)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Token 类型无效");
        }

        // 检查黑名单
        String jti = jwtTokenProvider.getJti(claims);
        if (tokenBlacklistMapper.existsByTokenJti(jti)) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_EXPIRED, "Token 已失效");
        }

        // 验证用户有效性
        String userId = jwtTokenProvider.getUserId(claims);
        User user = userMapper.selectActiveById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在或已禁用");
        }

        String newAccessToken = jwtTokenProvider.createAccessToken(user.getId(), getRoleCode(user.getRoleId()));

        TokenResponse response = new TokenResponse();
        response.setAccessToken(newAccessToken);
        response.setExpiresIn(jwtTokenProvider.getAccessTokenExpireMs() / 1000);

        log.debug("Token 刷新成功: userId={}", userId);
        return response;
    }

    /**
     * 登出 - 将当前 Token 加入黑名单
     *
     * @param token 当前请求的 Access Token（从 Authorization 头中提取）
     */
    @Transactional(rollbackFor = Exception.class)
    public void logout(String token) {
        if (token == null || token.isEmpty()) {
            return;
        }
        Claims claims = jwtTokenProvider.tryParseToken(token);
        if (claims == null) {
            return;
        }

        String jti = jwtTokenProvider.getJti(claims);
        Date expiration = jwtTokenProvider.getExpiration(claims);
        String userId = jwtTokenProvider.getUserId(claims);

        TokenBlacklist blacklist = new TokenBlacklist();
        blacklist.setTokenJti(jti);
        blacklist.setUserId(userId);
        blacklist.setExpiresAt(LocalDateTime.ofInstant(expiration.toInstant(), ZoneId.systemDefault()));
        tokenBlacklistMapper.insert(blacklist);

        log.info("用户登出: userId={}, jti={}", userId, jti);
    }

    /**
     * 获取当前登录用户信息
     */
    public UserResponse getCurrentUser(String userId) {
        User user = userMapper.selectActiveById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setDisplayName(user.getDisplayName());
        response.setRoleId(user.getRoleId());
        String roleCode = getRoleCode(user.getRoleId());
        response.setRole(roleCode);
        UserRole role = userRoleMapper.selectById(user.getRoleId());
        if (role != null) {
            response.setRoleName(role.getRoleName());
        }
        response.setIsActive(user.getIsActive());
        response.setLastLoginAt(user.getLastLoginAt());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }

    private UserBriefDTO toUserBrief(User user) {
        UserBriefDTO brief = new UserBriefDTO();
        brief.setId(user.getId());
        brief.setUsername(user.getUsername());
        brief.setDisplayName(user.getDisplayName());
        brief.setRole(getRoleCode(user.getRoleId()));
        return brief;
    }
}
