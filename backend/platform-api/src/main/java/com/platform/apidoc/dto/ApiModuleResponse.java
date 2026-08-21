package com.platform.apidoc.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 接口分组响应
 */
@Data
public class ApiModuleResponse {

    private Long id;
    private Long projectId;
    private Long parentId;
    private String name;
    private String servicePrefix;
    private String description;
    private String sourceType;
    private Integer isSystem;
    private Integer apiCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
