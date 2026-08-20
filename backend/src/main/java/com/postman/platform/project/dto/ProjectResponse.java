package com.postman.platform.project.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 项目响应
 */
@Data
public class ProjectResponse {

    private Long id;
    private String name;
    private String description;
    private String sourcePath;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
