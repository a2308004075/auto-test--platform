/**
 * @author HXN
 * @date 2026-08-22 13:27
 * @description CacheItem 响应 DTO
 */
package com.platform.sys.dto;

import lombok.Data;

/**
 * 缓存项响应
 */
@Data
public class CacheItemResponse {

    /**
     * 缓存键
     */
    private String key;

    /**
     * 缓存值
     */
    private String value;

    /**
     * 过期时间（秒），-1 表示永不过期，-2 表示 key 不存在
     */
    private Long ttl;
}
