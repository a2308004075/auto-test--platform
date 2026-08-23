/**
 * @author HXN
 * @date 2026-08-23
 * @description CI/CD Webhook 控制器
 */
package com.platform.execution.controller;

import com.platform.common.response.ApiResponse;
import com.platform.execution.dto.ExecutionResponse;
import com.platform.execution.dto.ExecutionStartRequest;
import com.platform.execution.service.ExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * CI/CD Webhook 接口
 *
 * <p>供外部 CI/CD 系统（Jenkins、GitLab CI、GitHub Actions 等）触发测试计划执行。
 * 此接口不需要 JWT 认证，通过 SecurityConfig 的 permitAll 放行。
 */
@RestController
@RequestMapping("/api/v1/webhook")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final ExecutionService executionService;

    /**
     * Webhook 触发测试计划执行
     *
     * <p>CI/CD 系统通过 POST 请求触发指定测试计划的执行。
     * 触发类型自动设为 WEBHOOK。
     *
     * @param planId        测试计划 ID
     * @param environmentId 可选环境 ID（覆盖计划默认环境）
     * @return 执行记录
     */
    @PostMapping("/execute/{planId}")
    public ApiResponse<ExecutionResponse> triggerExecution(
            @PathVariable Long planId,
            @RequestParam(required = false) Long environmentId) {
        log.info("Webhook 触发执行: planId={}, environmentId={}", planId, environmentId);
        ExecutionStartRequest request = new ExecutionStartRequest();
        request.setTriggerType("WEBHOOK");
        if (environmentId != null) {
            request.setEnvironmentId(environmentId);
        }
        return ApiResponse.ok(executionService.startExecution(planId, request));
    }
}
