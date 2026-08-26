/**
 * @author HXN
 * @date 2026-08-26
 * @description 接口关键字分组实体类
 */
package com.platform.keyword.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 接口关键字分组实体
 *
 * <p>对应数据库 api_keyword_group 表。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("api_keyword_group")
public class ApiKeywordGroup extends BaseEntity {

    /**
     * 所属项目 ID
     */
    private Long projectId;

    /**
     * 分组名称
     */
    private String name;

    /**
     * 创建人 ID
     */
    private Long createdBy;
}
