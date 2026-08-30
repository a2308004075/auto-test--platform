/**
 * @author HXN
 * @date 2026-08-30
 * @description 通用任务更新请求
 */
package com.platform.common.dto;

import lombok.Data;

import javax.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 通用任务更新请求（所有字段可选）
 */
@Data
public class TaskUpdateRequest {

    /**
     * 任务标题
     */
    @Size(max = 500, message = "任务标题长度不能超过 500")
    private String title;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 状态
     */
    @Size(max = 20, message = "状态长度不能超过 20")
    private String status;

    /**
     * 优先级
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
