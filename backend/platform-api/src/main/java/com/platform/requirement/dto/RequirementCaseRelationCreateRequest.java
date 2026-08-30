/**
 * @author HXN
 * @date 2026-08-30
 * @description 需求-用例关联创建请求 DTO
 */
package com.platform.requirement.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 需求-用例关联创建请求
 */
@Data
public class RequirementCaseRelationCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "用例类型不能为空")
    private String caseType;

    @NotNull(message = "用例 ID 不能为空")
    private Long caseId;
}
