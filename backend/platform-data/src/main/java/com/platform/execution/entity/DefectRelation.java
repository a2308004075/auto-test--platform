/**
 * @author HXN
 * @date 2026-08-30
 * @description 缺陷关联实体类
 */
package com.platform.execution.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 缺陷关联实体
 *
 * <p>对应数据库 defect_relation 表。</p>
 */
@Data
@TableName("defect_relation")
public class DefectRelation implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 缺陷 ID
     */
    private Long defectId;

    /**
     * 关联类型
     */
    private String relationType;

    /**
     * 关联目标类型
     */
    private String targetType;

    /**
     * 关联目标 ID
     */
    private Long targetId;

    /**
     * 关联目标标题快照
     */
    private String targetTitle;

    /**
     * 创建人 ID
     */
    private Long createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
