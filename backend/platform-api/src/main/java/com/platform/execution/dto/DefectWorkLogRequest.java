/**
 * @author HXN
 * @date 2026-08-30
 * @description 缺陷工时记录请求 DTO
 */
package com.platform.execution.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 缺陷工时记录请求
 */
@Data
public class DefectWorkLogRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private LocalDate logDate;

    @NotNull(message = "工时不能为空")
    private BigDecimal hours;

    private String workType;

    private String description;
}
