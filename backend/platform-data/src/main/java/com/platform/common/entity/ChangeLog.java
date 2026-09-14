/**
 * @author HXN
 * @date 2026-08-30
 * @description 变更记录实体类
 */
package com.platform.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 变更记录实体
 *
 * <p>对应数据库 change_log 表。用于记录业务对象字段的变更历史，
 * 支持按业务类型（biz_type）+ 业务对象 ID（biz_id）通用关联。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("change_log")
public class ChangeLog extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 业务类型：REQUIREMENT_ITEM-需求条目，MANUAL_CASE-手动化用例
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
}
