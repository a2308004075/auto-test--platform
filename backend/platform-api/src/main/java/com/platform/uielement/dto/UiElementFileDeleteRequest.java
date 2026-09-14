/**
 * @author HXN
 * @date 2026-08-30 14:00
 * @description 界面元素按文件删除请求 DTO
 */
package com.platform.uielement.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 按文件删除界面元素请求
 */
@Data
public class UiElementFileDeleteRequest {

    @NotNull(message = "仓库 ID 不能为空")
    private Long repositoryId;

    @NotBlank(message = "文件路径不能为空")
    @Size(max = 500, message = "文件路径长度不能超过 500")
    private String filePath;
}
