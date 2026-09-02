/**
 * @author HXN
 * @date 2026-08-30
 * @description 评论实体类
 */
package com.platform.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 评论实体
 *
 * <p>对应数据库 comment 表。支持按业务类型（biz_type）+ 业务对象 ID（biz_id）
 * 进行通用关联，可用于需求条目、手动化用例等业务对象的评论功能。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("comment")
public class Comment extends BaseEntity {

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
     * 评论内容
     */
    private String content;

    /**
     * 父评论 ID（null 表示一级评论）
     */
    private Long parentId;

    /**
     * 评论人 ID
     */
    private Long createdBy;
}
