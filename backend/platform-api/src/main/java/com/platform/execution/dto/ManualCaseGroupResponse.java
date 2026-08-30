/**
 * @author HXN
 * @date 2026-08-30
 * @description 手动用例分组响应 DTO
 */
package com.platform.execution.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 手动用例分组响应
 */
@Data
public class ManualCaseGroupResponse {

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
