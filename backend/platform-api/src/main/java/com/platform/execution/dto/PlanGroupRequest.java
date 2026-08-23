/**
 * @author HXN
 * @date 2026-08-23
 * @description 测试计划分组请求 DTO
 */
package com.platform.execution.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 创建/更新测试计划分组请求
 */
@Data
public class PlanGroupRequest {

    @NotBlank(message = "分组名称不能为空")
    private String name;

    private String description;

    /**
     * 父分组 ID（NULL=顶级分组）
     */
    private Long parentId;
}
