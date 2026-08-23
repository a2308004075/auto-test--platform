/**
 * @author HXN
 * @date 2026-08-23
 * @description 用例调试响应 DTO
 */
package com.platform.execution.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 用例调试结果响应
 */
@Data
public class CaseDebugResponse {

    /**
     * 执行状态：PASSED / FAILED / ERROR
     */
    private String status;

    /**
     * 结果消息
     */
    private String message;

    /**
     * 执行耗时（毫秒）
     */
    private long durationMs;

    /**
     * 步骤日志列表
     */
    private List<Map<String, Object>> stepLogs;
}
