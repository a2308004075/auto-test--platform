/**
 * @author HXN
 * @date 2026-08-30 14:00
 * @description 界面元素导入请求 DTO
 */
package com.platform.uielement.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 导入界面元素请求
 *
 * <p>从指定仓库的本地已拉取代码中解析前端源码交互元素。
 */
@Data
public class UiElementImportRequest {

    @NotNull(message = "仓库 ID 不能为空")
    private Long repositoryId;
}
