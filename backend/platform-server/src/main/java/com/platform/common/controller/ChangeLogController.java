/**
 * @author HXN
 * @date 2026-08-30
 * @description 变更记录控制器
 */
package com.platform.common.controller;

import com.platform.common.dto.ChangeLogResponse;
import com.platform.common.response.ApiResponse;
import com.platform.common.service.ChangeLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 变更记录接口
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ChangeLogController {

    private final ChangeLogService changeLogService;

    /**
     * 查询业务对象下的变更记录列表
     *
     * @param fieldName 字段名（可选，传空表示全部）
     */
    @GetMapping("/change-logs")
    public ApiResponse<List<ChangeLogResponse>> list(@RequestParam String bizType,
                                                      @RequestParam Long bizId,
                                                      @RequestParam(required = false) String fieldName) {
        return ApiResponse.ok(changeLogService.listByBiz(bizType, bizId, fieldName));
    }
}
