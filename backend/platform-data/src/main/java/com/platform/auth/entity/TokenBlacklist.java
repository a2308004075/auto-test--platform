/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description Token 黑名单实体类
 */
package com.platform.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.entity.BaseEntity;
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
