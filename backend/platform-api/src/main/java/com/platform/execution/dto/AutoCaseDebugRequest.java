/**
 * @author HXN
 * @date 2026-08-23
 * @description 自动化用例调试请求 DTO
 */
package com.platform.execution.dto;

import lombok.Data;

/**
 * 自动化用例调试请求
 */
@Data
public class AutoCaseDebugRequest {

    /**
     * 环境 ID（可选，不传则不加载环境变量）
     */
    private Long environmentId;
}
