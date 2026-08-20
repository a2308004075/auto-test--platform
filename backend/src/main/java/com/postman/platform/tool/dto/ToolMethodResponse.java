package com.postman.platform.tool.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ToolMethodResponse {

    private Long id;
    private Long projectId;
    private String name;
    private String category;
    private String description;
    private String paramDefinitions;
    private String returnType;
    private String code;
    private Boolean isActive;
    private String testInput;
    private String testResult;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
