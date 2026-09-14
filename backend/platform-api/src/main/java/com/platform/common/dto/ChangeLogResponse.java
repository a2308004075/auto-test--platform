/**
 * @author HXN
 * @date 2026-08-30
 * @description 变更记录响应
 */
package com.platform.common.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 变更记录响应
 */
@Data
public class ChangeLogResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 记录 ID
     */
    private Long id;

    /**
     * 业务类型
     */
    private String bizType;

    /**
     * 业务对象 ID
     */
    private Long bizId;

    /**
     * 变更字段名
     */
    private String fieldName;

    /**
     * 变更前值
     */
    private String oldValue;

    /**
     * 变更后值
     */
    private String newValue;

    /**
     * 操作人 ID
     */
    private Long createdBy;

    /**
     * 操作人显示名称
     */
    private String createdByName;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
