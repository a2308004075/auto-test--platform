/**
 * @author HXN
 * @date 2026-08-23 10:00
 * @description 套件分组响应 DTO
 */
package com.platform.execution.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 套件分组响应
 */
@Data
public class SuiteGroupDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long projectId;

    private Long parentId;

    private String name;

    private Integer sortNo;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * 该分组下的套件数量（附加统计）
     */
    private Long suiteCount;
}
