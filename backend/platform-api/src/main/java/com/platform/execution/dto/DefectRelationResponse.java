/**
 * @author HXN
 * @date 2026-08-30
 * @description 缺陷关联响应 DTO
 */
package com.platform.execution.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 缺陷关联响应
 */
@Data
public class DefectRelationResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long defectId;
    private String relationType;
    private String targetType;
    private Long targetId;
    private String targetTitle;
    private Long createdBy;
    private LocalDateTime createdAt;

    /**
     * 缺陷编号（按用例反查关联时填充）
     */
    private String defectNo;

    /**
     * 缺陷标题（按用例反查关联时填充）
     */
    private String defectTitle;

    /**
     * 缺陷状态（按用例反查关联时填充）
     */
    private String defectStatus;
}
