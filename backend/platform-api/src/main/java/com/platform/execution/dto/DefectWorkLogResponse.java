/**
 * @author HXN
 * @date 2026-08-30
 * @description 缺陷工时记录响应 DTO
 */
package com.platform.execution.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 缺陷工时记录响应
 */
@Data
public class DefectWorkLogResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long defectId;
    private Long userId;
    private String userName;
    private LocalDate logDate;
    private BigDecimal hours;
    private String workType;
    private String description;
    private LocalDateTime createdAt;
}
