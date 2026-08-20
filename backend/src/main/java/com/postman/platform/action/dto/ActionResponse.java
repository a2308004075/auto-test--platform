package com.postman.platform.action.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActionResponse {

    private String id;
    private String projectId;
    private String name;
    private String description;
    private String nodes;
    private String inputParams;
    private String outputParams;
    private Boolean isActive;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer referenceCount;
}
