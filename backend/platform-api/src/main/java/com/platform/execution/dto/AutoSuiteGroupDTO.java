/**
 * @author HXN
 * @date 2026-08-23 10:00
 * @description 自动化套件分组响应 DTO
 */
package com.platform.execution.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 自动化套件分组响应
 */
@Data
public class AutoSuiteGroupDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long projectId;

    private Long parentId;

    private String name;

    /**
     * 分组描述
     */
    private String description;

    private Integer sortNo;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * 该分组下的自动化套件数量（递归统计，含所有后代分组）
     */
    private Long suiteCount;
}
