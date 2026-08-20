package com.postman.platform.execution.dto;

import lombok.Data;

import java.util.List;

/**
 * 更新测试计划请求
 */
@Data
public class PlanUpdateRequest {

    private String name;

    private String description;

    private List<String> suiteIds;

    private String environmentId;

    private String scheduleCron;

    private Boolean isActive;
}
