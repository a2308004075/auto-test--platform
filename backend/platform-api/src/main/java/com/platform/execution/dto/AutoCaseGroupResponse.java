/**
 * @author HXN
 * @date 2026-08-23
 * @description 自动化用例分组响应 DTO
 */
package com.platform.execution.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 自动化用例分组响应
 */
@Data
public class AutoCaseGroupResponse {

    private Long id;
    private Long projectId;
    private Long parentId;
    private String name;
    private String description;
    private Integer isSystem;
    private Integer caseCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
