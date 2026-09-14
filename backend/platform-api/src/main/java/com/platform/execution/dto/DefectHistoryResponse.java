/**
 * @author HXN
 * @date 2026-08-30
 * @description 缺陷变更记录响应 DTO
 */
package com.platform.execution.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 缺陷变更记录响应
 */
@Data
public class DefectHistoryResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long defectId;
    private String fieldName;
    private String oldValue;
    private String newValue;
    private Long changedBy;
    private String changedByName;
    private LocalDateTime createdAt;
}
