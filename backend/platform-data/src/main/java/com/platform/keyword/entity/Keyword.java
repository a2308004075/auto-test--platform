package com.platform.keyword.entity;

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
    private String type;

    private Long projectId;

    /**
     * 指向源实体 ID（api_keyword.id / tool_method.id / action.id / test_case.id）
     */
    private Long refId;

    private String description;

    private Long createdBy;

    private Long updatedBy;
}
