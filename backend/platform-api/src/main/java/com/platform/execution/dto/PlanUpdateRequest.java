/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 测试计划更新请求 DTO
 */
package com.platform.execution.dto;

import lombok.Data;

import java.util.List;

/**
 * 更新测试计划请求
 */
@Data
public class PlanUpdateRequest {

    private String name;

    private String description;

    /**
     * 所属分组 ID
     */
    private Long groupId;

    /**
     * 是否清除分组（设为 true 时将 groupId 置为 null，计划归入"未分组"）
     */
    private Boolean clearGroup;

    private List<Long> autoSuiteIds;

    private Long environmentId;

    private String scheduleCron;

    /**
     * 触发方式：MANUAL / SCHEDULED / CI
     */
    private String triggerType;

    private Integer isActive;
}
