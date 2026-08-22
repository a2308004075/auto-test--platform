/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 测试用例响应 DTO
 */
package com.platform.execution.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 测试用例响应
 */
@Data
public class CaseResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long suiteId;
    private String name;
    private String description;
    private String preconditions;
    private String setupSteps;
    private String teardownSteps;
    private String steps;
    private String priority;
    private Integer timeout;
    private Integer isActive;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
