/**
 * @author HXN
 * @date 2026-08-22 13:27
 * @description CacheSetRequest
 */
package com.platform.sys.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 缓存设置请求
 */
@Data
public class CacheSetRequest {

    /**
     * 缓存键
     */
    @NotBlank(message = "缓存键不能为空")
    private String key;

    /**
     * 缓存值
     */
    @NotBlank(message = "缓存值不能为空")
    private String value;

    /**
     * 过期时间（秒），为空或 0 表示永不过期
     */
    private Long ttl;
}
