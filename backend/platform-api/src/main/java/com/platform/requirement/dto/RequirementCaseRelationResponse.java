/**
 * @author HXN
 * @date 2026-08-30
 * @description 需求-用例关联响应 DTO
 */
package com.platform.requirement.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 需求-用例关联响应
 *
 * <p>正查（需求条目视角）时填充用例标题快照；
 * 反查（用例视角）时填充需求条目标题及所属版本信息。
 */
@Data
public class RequirementCaseRelationResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long requirementItemId;

    /**
     * 用例类型：MANUAL_CASE-手动化用例，AUTO_CASE-自动化用例
     */
    private String caseType;

    private Long caseId;

    /**
     * 用例标题快照
     */
    private String caseTitle;

    /**
     * 需求条目标题（反查时填充）
     */
    private String requirementItemTitle;

    /**
     * 需求条目状态（反查时填充）
     */
    private String requirementItemStatus;

    /**
     * 所属需求版本 ID（反查时填充）
     */
    private Long versionId;

    /**
     * 所属需求版本名称（反查时填充）
     */
    private String versionName;

    private Long createdBy;

    private String createdByName;

    private LocalDateTime createdAt;
}
