/**
 * @author HXN
 * @date 2026-08-30
 * @description 缺陷关联创建请求 DTO
 */
package com.platform.execution.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 缺陷关联创建请求
 */
@Data
public class DefectRelationCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String relationType;

    @NotBlank(message = "关联目标类型不能为空")
    private String targetType;

    @NotNull(message = "关联目标 ID 不能为空")
    private Long targetId;

    private String targetTitle;
}
