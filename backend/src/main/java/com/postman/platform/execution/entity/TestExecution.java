package com.postman.platform.execution.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 测试执行记录实体
 *
 * <p>对应数据库 test_execution 表。该表无 updated_at 字段，不继承 BaseEntity。
 */
@Data
@TableName("test_execution")
public class TestExecution implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long planId;

    private Long environmentId;

    /**
     * 触发方式：MANUAL / SCHEDULED / CI
     */
    private String triggerType;

    /**
     * 执行状态：PENDING / RUNNING / COMPLETED / FAILED / CANCELLED
     */
    private String status;

    private Integer totalCases;

    private Integer passedCases;

    private Integer failedCases;

    private Integer skippedCases;

    /**
     * 总耗时（毫秒）
     */
    private Integer durationMs;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private Long triggeredBy;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
