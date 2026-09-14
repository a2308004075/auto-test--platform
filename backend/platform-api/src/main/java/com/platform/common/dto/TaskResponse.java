/**
 * @author HXN
 * @date 2026-08-30
 * @description 通用任务响应 DTO
 */
package com.platform.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 通用任务响应
 */
@Data
public class TaskResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /**
     * 所属项目 ID
     */
    private Long projectId;

    /**
     * 任务类型
     */
    private String taskType;

    /**
     * 任务标题
     */
    private String title;

    /**
     * 任务描述
     */
    private String description;

    /**
     * 状态
     */
    private String status;

    /**
     * 优先级
     */
    private String priority;

    /**
     * 负责人 ID
     */
    private Long assigneeId;

    /**
     * 负责人显示名称
     */
    private String assigneeName;

    /**
     * 关联业务类型
     */
    private String bizType;

    /**
     * 关联业务 ID
     */
    private Long bizId;

    /**
     * 截止日期
     */
    private LocalDate dueDate;

    /**
     * 创建人 ID
     */
    private Long createdBy;

    /**
     * 创建人显示名称
     */
    private String createdByName;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
