/**
 * @author HXN
 * @date 2026-08-30
 * @description 前端错误日志上报接口
 */
package com.platform.common.controller;

import com.platform.common.dto.FrontendLogRequest;
import com.platform.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 前端错误日志上报接口
 *
 * <p>接收前端运行时错误并写入 backend/log/frontend-err.log，
 * 由 Logback 独立 logger「com.platform.frontend」输出。
 */
@Slf4j(topic = "com.platform.frontend")
@RestController
@RequestMapping("/api/v1/frontend-log")
public class FrontendLogController {

    /**
     * 上报前端错误日志
     */
    @PostMapping
    public ApiResponse<Void> report(@Valid @RequestBody FrontendLogRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("[前端错误]").append(request.getType())
          .append(" | 页面: ").append(nullSafe(request.getUrl()))
          .append(" | 消息: ").append(request.getMessage());

        if (request.getExtra() != null && !request.getExtra().isEmpty()) {
            sb.append(" | 附加: ").append(request.getExtra());
        }
        if (request.getUserAgent() != null && !request.getUserAgent().isEmpty()) {
            sb.append(" | UA: ").append(request.getUserAgent());
        }

        String logMessage = sb.toString();

        if (request.getStack() != null && !request.getStack().isEmpty()) {
            log.error("{}\n堆栈: {}", logMessage, request.getStack());
        } else {
            log.error(logMessage);
        }

        return ApiResponse.ok();
    }

    private String nullSafe(String value) {
        return value != null ? value : "-";
    }
}
