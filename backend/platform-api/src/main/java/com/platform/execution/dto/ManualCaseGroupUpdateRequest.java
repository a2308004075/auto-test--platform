/**
 * @author HXN
 * @date 2026-08-30
 * @description 手动用例分组更新请求 DTO
 */
package com.platform.execution.dto;

import lombok.Data;

import javax.validation.constraints.Size;

/**
 * 更新手动用例分组请求
 */
@Data
public class ManualCaseGroupUpdateRequest {

    /**
     * 父分组 ID（null=根分组）
     */
    private Long parentId;

    @Size(max = 100, message = "分组名称长度不能超过 100")
    private String name;

    @Size(max = 500, message = "描述长度不能超过 500")
    private String description;
}
