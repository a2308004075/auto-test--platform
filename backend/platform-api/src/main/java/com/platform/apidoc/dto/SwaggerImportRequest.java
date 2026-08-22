/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description Swagger 导入请求 DTO
 */
package com.platform.apidoc.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * Swagger 导入请求
 */
@Data
public class SwaggerImportRequest {

    @NotNull(message = "项目 ID 不能为空")
    private Long projectId;

    @NotNull(message = "分组 ID 不能为空")
    private Long moduleId;

    /**
     * Swagger 2.0 JSON 内容
     */
    @NotBlank(message = "Swagger JSON 内容不能为空")
    private String swaggerJson;

    /**
     * 导入模式：FULL（全量）/ INCREMENTAL（增量）
     */
    private String importMode;
}
