/**
 * @author HXN
 * @date 2026-08-22 13:28
 * @description 缓存管理服务
 */
package com.platform.sys.service;

import com.platform.common.exception.BusinessException;
import com.platform.common.exception.ErrorCode;
import com.platform.sys.dto.CacheItemResponse;
import com.platform.sys.dto.CacheSetRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 缓存管理服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 根据 key 精确查询缓存
     */
    public CacheItemResponse getByKey(String key) {
        Boolean hasKey = redisTemplate.hasKey(key);
        if (hasKey == null || !hasKey) {
            throw new BusinessException(ErrorCode.CACHE_KEY_NOT_FOUND, "缓存键不存在：" + key);
        }
        return buildCacheItem(key);
    }

    /**
     * 模糊搜索缓存键（使用 SCAN 命令，安全不影响生产）
     */
    public List<CacheItemResponse> search(String pattern, int limit) {
        List<CacheItemResponse> results = new ArrayList<>();
        ScanOptions options = ScanOptions.scanOptions()
                .match("*" + pattern + "*")
                .count(100)
                .build();

        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            int count = 0;
            while (cursor.hasNext() && count < limit) {
                String key = cursor.next();
                try {
                    results.add(buildCacheItem(key));
                    count++;
                } catch (Exception e) {
                    log.warn("读取缓存键 {} 失败: {}", key, e.getMessage());
                }
            }
        }
        return results;
    }

    /**
     * 设置缓存
     */
    public void set(CacheSetRequest request) {
        String key = request.getKey();
        String value = request.getValue();
        Long ttl = request.getTtl();

        if (ttl != null && ttl > 0) {
            redisTemplate.opsForValue().set(key, value, ttl, TimeUnit.SECONDS);
        } else {
            redisTemplate.opsForValue().set(key, value);
        }
    }

    /**
     * 删除缓存
     */
    public void delete(String key) {
        Boolean deleted = redisTemplate.delete(key);
        if (deleted == null || !deleted) {
            throw new BusinessException(ErrorCode.CACHE_KEY_NOT_FOUND, "缓存键不存在：" + key);
        }
    }

    // ===== 私有方法 =====

    private CacheItemResponse buildCacheItem(String key) {
        CacheItemResponse item = new CacheItemResponse();
        item.setKey(key);
        Object value = redisTemplate.opsForValue().get(key);
        item.setValue(value != null ? value.toString() : "");
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        item.setTtl(ttl != null ? ttl : -2L);
        return item;
    }
}
