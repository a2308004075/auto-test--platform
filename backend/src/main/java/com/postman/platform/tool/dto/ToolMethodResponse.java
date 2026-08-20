package com.postman.platform.tool.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ToolMethodResponse {

    private String id;
    private String projectId;
    private String name;
    private String category;
    private String description;
    private String paramDefinitions;
    private String returnType;
    private String code;
    private Boolean isActive;
    private String testInput;
    private String testResult;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
