package com.postman.platform.apidoc.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 接口分组响应
 */
@Data
public class ApiModuleResponse {

    private String id;
    private String projectId;
    private String name;
    private String servicePrefix;
    private String description;
    private String sourceType;
    private Boolean isSystem;
    private Integer apiCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
