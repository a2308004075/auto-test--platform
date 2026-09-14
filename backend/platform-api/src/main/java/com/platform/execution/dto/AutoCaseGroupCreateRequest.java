/**
 * @author HXN
 * @date 2026-08-23
 * @description 自动化用例分组创建请求 DTO
 */
package com.platform.execution.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 创建自动化用例分组请求
 */
@Data
public class AutoCaseGroupCreateRequest {

    private Long projectId;

    /**
     * 父分组 ID（null=根分组）
     */
    private Long parentId;

    @NotBlank(message = "分组名称不能为空")
    @Size(max = 100, message = "分组名称长度不能超过 100")
    private String name;

    @Size(max = 500, message = "描述长度不能超过 500")
    private String description;
}
