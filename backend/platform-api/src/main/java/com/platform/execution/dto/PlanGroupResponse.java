/**
 * @author HXN
 * @date 2026-08-23
 * @description 测试计划分组响应 DTO
 */
package com.platform.execution.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 测试计划分组响应
 */
@Data
public class PlanGroupResponse {

    private Long id;

    private Long projectId;

    private String name;

    private String description;

    /**
     * 父分组 ID（NULL=顶级）
     */
    private Long parentId;

    private Integer sortOrder;

    private LocalDateTime createdAt;

    /**
     * 该分组下的计划数量（含子分组递归）
     */
    private Integer planCount;
}
