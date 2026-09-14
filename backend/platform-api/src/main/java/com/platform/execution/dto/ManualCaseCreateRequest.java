/**
 * @author HXN
 * @date 2026-08-30
 * @description 手动化用例创建请求 DTO
 */
package com.platform.execution.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 手动化用例创建请求
 */
@Data
public class ManualCaseCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "用例标题不能为空")
    @Size(max = 200, message = "用例标题长度不能超过 200")
    private String title;

    private String preconditions;

    private String operationSteps;

    private String expectedResult;

    /**
     * 用例类型：NORMAL-正常，EXCEPTION-异常
     */
    private String caseType;

    /**
     * 优先级：高/中/低
     */
    private String priority;

    /**
     * 所属分组 ID
     */
    private Long groupId;

    /**
     * 测试环境是否执行（1-是，0-否）
     */
    private Integer runInTestEnv;

    /**
     * 生产环境是否执行（1-是，0-否）
     */
    private Integer runInProdEnv;

    /**
     * 用例状态（1-使用，0-废弃）
     */
    private Integer caseStatus;
}
