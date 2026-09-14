/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 自动化用例创建请求 DTO
 */
package com.platform.execution.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 自动化用例创建请求
 */
@Data
public class AutoCaseCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 所属自动化套件 ID（可由 path 注入）
     */
    private Long autoSuiteId;

    @NotBlank(message = "自动化用例名称不能为空")
    @Size(max = 100, message = "自动化用例名称长度不能超过 100")
    private String name;

    @Size(max = 1000, message = "描述长度不能超过 1000")
    private String description;

    private String preconditions;

    /**
     * 自动化用例级 Setup 步骤树（JSON）
     */
    private String setupSteps;

    private String teardownSteps;

    /**
     * 自动化用例步骤树（JSON，核心）
     */
    private String steps;

    private String priority;

    /**
     * 所属分组 ID
     */
    private Long groupId;

    /**
     * 标签列表（JSON）
     */
    private String tags;

    /**
     * 超时秒数，默认 30
     */
    private Integer timeout;
}
