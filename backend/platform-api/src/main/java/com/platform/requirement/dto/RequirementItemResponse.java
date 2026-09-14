/**
 * @author HXN
 * @date 2026-08-30
 * @description 需求条目响应 DTO
 */
package com.platform.requirement.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 需求条目响应
 */
@Data
public class RequirementItemResponse {

    private Long id;

    private Long versionId;

    private String title;

    private String description;

    private String reqType;

    private String priority;

    private String status;

    private String assignee;

    private LocalDate deadline;

    private Integer sortOrder;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
