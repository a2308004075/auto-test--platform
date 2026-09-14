/**
 * @author HXN
 * @date 2026-08-30
 * @description 通用任务创建请求
 */
package com.platform.common.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 通用任务创建请求
 */
@Data
public class TaskCreateRequest {

    /**
     * 所属项目 ID
     */
    @NotNull(message = "项目 ID 不能为空")
    private Long projectId;

    /**
     * 任务类型
     */
    @NotBlank(message = "任务类型不能为空")
    @Size(max = 30, message = "任务类型长度不能超过 30")
    private String taskType;

    /**
     * 任务标题
     */
    @NotBlank(message = "任务标题不能为空")
    @Size(max = 500, message = "任务标题长度不能超过 500")
    private String title;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 优先级：高 / 中 / 低
     */
    @Size(max = 10, message = "优先级长度不能超过 10")
    private String priority;

    /**
     * 负责人 ID
     */
    private Long assigneeId;

    /**
     * 关联业务类型
     */
    @Size(max = 30, message = "业务类型长度不能超过 30")
    private String bizType;

    /**
     * 关联业务 ID
     */
    private Long bizId;

    /**
     * 截止日期
     */
    private LocalDate dueDate;
}
