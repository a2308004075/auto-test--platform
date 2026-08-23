/**
 * @author HXN
 * @date 2026-08-23
 * @description Swagger URL 同步请求 DTO
 */
package com.platform.apidoc.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 从 URL 同步 Swagger 文档请求
 */
@Data
public class SwaggerSyncRequest {

    /**
     * Swagger JSON 文档 URL（如 http://localhost:8080/v2/api-docs）
     */
    @NotBlank(message = "Swagger URL 不能为空")
    private String url;

    /**
     * 目标分组 ID
     */
    @NotNull(message = "目标分组不能为空")
    private Long moduleId;

    /**
     * 项目 ID（由 Controller 层注入）
     */
    private Long projectId;
}
