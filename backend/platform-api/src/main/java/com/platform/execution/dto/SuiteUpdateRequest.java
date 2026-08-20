package com.platform.execution.dto;

import lombok.Data;

import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 测试套件更新请求
 */
@Data
public class SuiteUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Size(max = 100, message = "套件名称长度不能超过 100")
    private String name;

    @Size(max = 1000, message = "描述长度不能超过 1000")
    private String description;

    private String priority;

    private String tags;

    private Boolean enableOnceSetupTeardown;

    private String onceSetupSteps;

    private String onceTeardownSteps;

    private Boolean enablePerCaseSetupTeardown;

    private String perCaseSetupSteps;

    private String perCaseTeardownSteps;
}
