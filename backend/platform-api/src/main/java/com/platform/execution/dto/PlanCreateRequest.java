/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 测试计划创建请求 DTO
 */
package com.platform.execution.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * 创建测试计划请求
 */
@Data
public class PlanCreateRequest {

    private Long projectId;

    @NotBlank(message = "计划名称不能为空")
    private String name;

    private String description;

    /**
     * 所属分组 ID
     */
    private Long groupId;

    /**
     * 关联的自动化套件 ID 列表
     */
    private List<Long> autoSuiteIds;

    /**
     * 关联的手动化用例 ID 列表
     */
    private List<Long> manualCaseIds;

    /**
     * 默认执行环境 ID
     */
    private Long environmentId;

    /**
     * 定时执行 cron 表达式
     */
    private String scheduleCron;

    /**
     * 触发方式：MANUAL / SCHEDULED / CI
     */
    private String triggerType;
}
