package com.platform.action.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActionResponse {

    private Long id;
    private Long projectId;
    private String name;
    private String description;
    private String nodes;
    private String inputParams;
    private String outputParams;
    private Boolean isActive;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer referenceCount;
}
