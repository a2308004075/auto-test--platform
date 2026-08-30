/**
 * @author HXN
 * @date 2026-08-30
 * @description 缺陷分组响应 DTO
 */
package com.platform.execution.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 缺陷分组响应
 */
@Data
public class DefectGroupResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long projectId;
    private Long parentId;
    private String name;
    private String description;
    private Integer isSystem;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 该分组下（含子分组）的缺陷数量
     */
    private Integer defectCount;
}
