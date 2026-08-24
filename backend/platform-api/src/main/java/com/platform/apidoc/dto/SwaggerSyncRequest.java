/**
 * @author HXN
 * @date 2026-08-23
 * @description Swagger URL 同步请求 DTO
 */
package com.platform.apidoc.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * 从 URL 同步 Swagger 文档请求
 */
@Data
public class SwaggerSyncRequest {

    /**
     * OpenAPI/Swagger JSON 文档 URL
     * 支持 doc.html 页面地址（自动探测 JSON 端点），也可直接填写 /v3/api-docs 或 /v2/api-docs
     */
    @NotBlank(message = "Swagger URL 不能为空")
    private String url;

    /**
     * 目标分组 ID
     */
    @NotNull(message = "目标分组不能为空")
    private Long moduleId;

    /**
     * 自定义请求头（可选，用于认证等场景，如 Authorization: Bearer xxx）
     */
    private Map<String, String> headers;

    /**
     * 项目 ID（由 Controller 层注入）
     */
    private Long projectId;
}
