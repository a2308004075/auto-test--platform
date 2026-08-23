/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 测试套件创建请求 DTO
 */
package com.platform.execution.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 测试套件创建请求
 */
@Data
public class SuiteCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "套件名称不能为空")
    @Size(max = 100, message = "套件名称长度不能超过 100")
    private String name;

    @Size(max = 1000, message = "描述长度不能超过 1000")
    private String description;

    /**
     * 优先级 P0/P1/P2/P3，默认 P2
     */
    private String priority;

    /**
     * 所属分组 ID
     */
    private Long groupId;

    /**
     * 标签列表（JSON 数组字符串）
     */
    private String tags;

    private Integer enableOnceSetupTeardown;

    /**
     * 套件级·整体 Setup 步骤树（JSON）
     */
    private String onceSetupSteps;

    private String onceTeardownSteps;

    private Integer enablePerCaseSetupTeardown;

    private String perCaseSetupSteps;

    private String perCaseTeardownSteps;
}
