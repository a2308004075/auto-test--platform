/**
 * @author HXN
 * @date 2026-08-23 10:00
 * @description 自动化套件通过率响应 DTO
 */
package com.platform.execution.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 自动化套件通过率响应
 */
@Data
public class AutoSuitePassRateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long autoSuiteId;

    /**
     * 通过率百分比（0-100），无执行记录时为 -1
     */
    private Integer passRate;

    /**
     * 参与统计的自动化用例总数
     */
    private Integer totalCases;
}
