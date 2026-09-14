/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 自动化用例更新请求 DTO
 */
package com.platform.execution.dto;

import lombok.Data;

import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 自动化用例更新请求
 */
@Data
public class AutoCaseUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Size(max = 100, message = "自动化用例名称长度不能超过 100")
    private String name;

    @Size(max = 1000, message = "描述长度不能超过 1000")
    private String description;

    private String preconditions;

    private String setupSteps;

    private String teardownSteps;

    private String steps;

    private String priority;

    private Integer timeout;

    private Integer isActive;

    /**
     * 所属分组 ID
     */
    private Long groupId;

    /**
     * 标签列表（JSON）
     */
    private String tags;
}
