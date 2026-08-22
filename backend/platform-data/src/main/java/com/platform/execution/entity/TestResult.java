/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 测试结果实体类
 */
package com.platform.execution.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 测试结果明细实体
 *
 * <p>对应数据库 test_result 表。该表无 created_at/updated_at 字段，不继承 BaseEntity。
 */
@Data
@TableName("test_result")
public class TestResult implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long executionId;

    private Long caseId;

    /**
     * 用例执行结果：PASSED / FAILED / SKIPPED / ERROR
     */
    private String status;

    /**
     * 实际结果摘要
     */
    private String actualResult;

    /**
     * 预期结果摘要
     */
    private String expectedResult;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 执行日志（JSON，每步骤 req/res 详情）
     */
    private String logs;

    /**
     * 执行耗时（毫秒）
     */
    private Integer durationMs;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;
}
