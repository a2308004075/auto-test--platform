package com.postman.platform.apidoc.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 创建接口请求
 */
@Data
public class ApiCreateRequest {

    @NotBlank(message = "项目 ID 不能为空")
    private String projectId;

    @NotBlank(message = "分组 ID 不能为空")
    private String moduleId;

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
     * 响应体 Schema（JSON 字符串）
     */
    private String responseBody;

    /**
     * 请求头（JSON 数组字符串）
     */
    private String headers;

    @Size(max = 2000, message = "描述长度不能超过 2000")
    private String description;
}
