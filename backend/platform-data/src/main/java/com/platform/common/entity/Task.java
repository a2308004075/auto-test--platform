/**
 * @author HXN
 * @date 2026-08-30
 * @description 通用任务实体类
 */
package com.platform.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 通用任务实体
 *
 * <p>对应数据库 task 表。支持多种任务类型（需求评审、用例评审、缺陷处理等），
 * 通过 biz_type + biz_id 关联具体业务对象。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("task")
public class Task extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 所属项目 ID
     */
    private Long projectId;

    /**
     * 任务类型：REQUIREMENT_REVIEW / CASE_REVIEW / REQUIREMENT_MODIFY / CASE_MODIFY / CASE_EXECUTION / DEFECT_HANDLING
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
     * 状态：PENDING / IN_PROGRESS / COMPLETED / CANCELLED
     */
    private String status;

    /**
     * 优先级：高 / 中 / 低
     */
    private String priority;

    /**
     * 负责人 ID
     */
    private Long assigneeId;

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
}
