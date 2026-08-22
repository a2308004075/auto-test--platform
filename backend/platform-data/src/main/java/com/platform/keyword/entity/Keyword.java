/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 关键字实体类
 */
package com.platform.keyword.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 统一关键字实体（对应 keyword 表）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("keyword")
public class Keyword extends BaseEntity {

    private String name;

    /**
     * 关键字类型：API / TOOL / ACTION / TEST_CASE
     */
    @TableField("keyword_type")
    private String type;

    private Long projectId;

    /**
     * 指向源实体 ID（api_keyword.id / tool_method.id / action.id / test_case.id）
     */
    private Long refId;

    private String description;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签列表（JSON 数组）
     */
    private String tags;

    private Long createdBy;

    private Long updatedBy;
}
