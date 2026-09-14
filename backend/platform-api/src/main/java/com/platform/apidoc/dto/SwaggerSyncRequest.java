/**
 * @author HXN
 * @date 2026-08-23
 * @description Swagger 同步请求 DTO
 */
package com.platform.apidoc.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * Swagger 同步请求
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
     * 拉取 Swagger 文档时使用的请求头（可选，如 Basic Auth 的 Authorization）
     */
    private Map<String, String> headers;

    /**
     * 导入后统一附加到各接口的请求头（可选，Key: Value）
     */
    private Map<String, String> defaultHeaders;

    /**
     * 导入附加默认 host 前缀（可选，如 ${host}，附加到各接口 URL 前）
     */
    private String hostPrefix;

    /**
     * 项目 ID（由 Controller 层注入）
     */
    private Long projectId;
}
