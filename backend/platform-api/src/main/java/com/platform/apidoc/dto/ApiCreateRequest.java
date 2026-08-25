/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description API 创建请求 DTO
 */
package com.platform.apidoc.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 创建接口请求
 */
@Data
public class ApiCreateRequest {

    @NotNull(message = "项目 ID 不能为空")
    private Long projectId;

    @NotNull(message = "分组 ID 不能为空")
    private Long moduleId;

    @NotBlank(message = "接口名称不能为空")
    @Size(max = 200, message = "接口名称长度不能超过 200")
    private String name;

    @Size(max = 100, message = "服务名长度不能超过 100")
    private String service;

    @NotBlank(message = "HTTP 方法不能为空")
    private String httpMethod;

    @NotBlank(message = "路径不能为空")
    @Size(max = 500, message = "路径长度不能超过 500")
    private String path;

    /**
     * 请求参数（JSON 数组字符串）
     */
    private String requestParams;

    /**
     * 请求体 Schema（JSON 字符串）
     */
    private String requestBody;

    /**
     * 请求体格式：none/form_data/x_www_form_urlencoded/raw/binary/graphql
     */
    private String bodyType;

    /**
     * raw 子类型：text/javascript/json/html/xml
     */
    private String rawType;

    /**
     * 响应体 Schema（JSON 字符串）
     */
    private String responseBody;

    /**
     * 请求头（JSON 数组字符串）
     */
    private String headers;

    /**
     * 默认 Content-Type
     */
    private String contentType;

    @Size(max = 2000, message = "描述长度不能超过 2000")
    private String description;
}
