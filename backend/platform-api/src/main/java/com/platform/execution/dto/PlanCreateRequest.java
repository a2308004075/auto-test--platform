package com.platform.execution.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 创建测试计划请求
 */
@Data
public class PlanCreateRequest {

    @NotNull(message = "项目 ID 不能为空")
    private Long projectId;

    @NotBlank(message = "计划名称不能为空")
    private String name;

    private String description;

    /**
     * 关联的测试套件 ID 列表
     */
    private List<Long> suiteIds;

    /**
     * 默认执行环境 ID
     */
    private Long environmentId;

    /**
     * 定时执行 cron 表达式
     */
    private String scheduleCron;
}
