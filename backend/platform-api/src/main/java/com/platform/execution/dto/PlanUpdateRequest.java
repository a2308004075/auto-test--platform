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

    private List<Long> suiteIds;

    private Long environmentId;

    private String scheduleCron;

    private Integer isActive;
}
