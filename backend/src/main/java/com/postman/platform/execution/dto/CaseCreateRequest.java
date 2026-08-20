package com.postman.platform.execution.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 测试用例创建请求
 */
@Data
public class CaseCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 所属套件 ID（可由 path 注入）
     */
    private Long suiteId;

    @NotBlank(message = "用例名称不能为空")
    @Size(max = 100, message = "用例名称长度不能超过 100")
    private String name;

    @Size(max = 1000, message = "描述长度不能超过 1000")
    private String description;

    private String preconditions;

    /**
     * 用例级 Setup 步骤树（JSON）
     */
    private String setupSteps;

    private String teardownSteps;

    /**
     * 用例步骤树（JSON，核心）
     */
    private String steps;

    private String priority;

    /**
     * 超时秒数，默认 30
     */
    private Integer timeout;
}
