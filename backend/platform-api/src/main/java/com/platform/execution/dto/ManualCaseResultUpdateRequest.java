/**
 * @author HXN
 * @date 2026-09-02
 * @description 手动化用例执行结果更新请求 DTO
 */
package com.platform.execution.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 手动化用例执行结果更新请求
 */
@Data
public class ManualCaseResultUpdateRequest {

    /**
     * 手动化用例结果 ID（test_result.id）
     */
    @NotNull(message = "结果 ID 不能为空")
    private Long resultId;

    /**
     * 执行结果：PASSED / FAILED / SKIPPED
     */
    @NotBlank(message = "执行结果不能为空")
    private String status;

    /**
     * 实际结果摘要
     */
    private String actualResult;

    /**
     * 错误信息
     */
    private String errorMessage;
}
