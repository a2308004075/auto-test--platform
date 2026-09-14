/**
 * @author HXN
 * @date 2026-08-30
 * @description 缺陷变更记录实体类
 */
package com.platform.execution.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 缺陷变更记录实体
 *
 * <p>对应数据库 defect_history 表。</p>
 */
@Data
@TableName("defect_history")
public class DefectHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 缺陷 ID
     */
    private Long defectId;

    /**
     * 变更字段
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
     * 变更人 ID
     */
    private Long changedBy;

    /**
     * 变更时间
     */
    private LocalDateTime createdAt;
}
