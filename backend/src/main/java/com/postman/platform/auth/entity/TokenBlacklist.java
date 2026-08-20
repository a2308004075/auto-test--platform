package com.postman.platform.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.postman.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Token 黑名单实体 - 用于登出后使 Token 失效
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("token_blacklist")
public class TokenBlacklist extends BaseEntity {

    /**
     * Token 唯一标识（JWT ID）
     */
    private String tokenJti;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * Token 原始过期时间（用于定期清理）
     */
    private LocalDateTime expiresAt;
}
