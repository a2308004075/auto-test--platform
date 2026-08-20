package com.platform.execution.dto;

import lombok.Data;

import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 测试用例更新请求
 */
@Data
public class CaseUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Size(max = 100, message = "用例名称长度不能超过 100")
    private String name;

    @Size(max = 1000, message = "描述长度不能超过 1000")
    private String description;

    private String preconditions;

    private String setupSteps;

    private String teardownSteps;

    private String steps;

    private String priority;

    private Integer timeout;

    private Boolean isActive;
}
