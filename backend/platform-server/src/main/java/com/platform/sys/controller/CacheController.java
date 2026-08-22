/**
 * @author HXN
 * @date 2026-08-22 13:28
 * @description 缓存管理控制器
 */
package com.platform.sys.controller;

import com.platform.common.response.ApiResponse;
import com.platform.sys.dto.CacheItemResponse;
import com.platform.sys.dto.CacheSetRequest;
import com.platform.sys.service.CacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

/**
 * 缓存管理接口（仅 ADMIN）
 */
@RestController
@RequestMapping("/api/v1/sys/cache")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class CacheController {

    private final CacheService cacheService;

    /**
     * 精确查询缓存
     */
    @GetMapping("/{key}")
    public ApiResponse<CacheItemResponse> getByKey(@PathVariable String key) {
        return ApiResponse.ok(cacheService.getByKey(key));
    }

    /**
     * 模糊搜索缓存键
     */
    @GetMapping("/search")
    public ApiResponse<List<CacheItemResponse>> search(
            @RequestParam String pattern,
            @RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.ok(cacheService.search(pattern, limit));
    }

    /**
     * 设置缓存
     */
    @PostMapping
    public ApiResponse<Void> set(@Valid @RequestBody CacheSetRequest request) {
        cacheService.set(request);
        return ApiResponse.ok(null);
    }

    /**
     * 删除缓存
     */
    @DeleteMapping("/{key}")
    public ApiResponse<Void> delete(@PathVariable String key) {
        cacheService.delete(key);
        return ApiResponse.ok(null);
    }
}
