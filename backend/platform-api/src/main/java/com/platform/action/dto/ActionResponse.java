/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description Action 响应 DTO
 */
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
    private Integer isActive;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer referenceCount;
}
