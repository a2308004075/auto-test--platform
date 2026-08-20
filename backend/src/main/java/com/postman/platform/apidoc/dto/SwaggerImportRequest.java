package com.postman.platform.apidoc.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * Swagger 导入请求
 */
@Data
public class SwaggerImportRequest {

    @NotBlank(message = "项目 ID 不能为空")
    private String projectId;

    @NotBlank(message = "分组 ID 不能为空")
    private String moduleId;

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
