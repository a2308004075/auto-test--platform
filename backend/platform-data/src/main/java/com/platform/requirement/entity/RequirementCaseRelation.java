/**
 * @author HXN
 * @date 2026-08-30
 * @description 需求-用例关联实体类
 */
package com.platform.requirement.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 需求-用例关联实体
 *
 * <p>对应数据库 requirement_case_relation 表。记录需求条目与用例（手动/自动）的多对多关联。
 * 用例类型：MANUAL_CASE（手动化用例）、AUTO_CASE（自动化用例）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("requirement_case_relation")
public class RequirementCaseRelation extends BaseEntity {

    /**
     * 需求条目 ID
     */
    private Long requirementItemId;

    /**
     * 用例类型：MANUAL_CASE-手动化用例，AUTO_CASE-自动化用例
     */
    private String caseType;

    /**
     * 用例 ID
     */
    private Long caseId;

    /**
     * 用例标题快照
     */
    private String caseTitle;

    /**
     * 创建人 ID
     */
    private Long createdBy;
}
