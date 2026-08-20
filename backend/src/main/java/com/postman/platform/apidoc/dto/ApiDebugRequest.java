package com.postman.platform.apidoc.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.Map;

/**
 * 接口调试请求
 */
@Data
public class ApiDebugRequest {

    @NotBlank(message = "环境 ID 不能为空")
    private String environmentId;

    /**
     * 路径参数（键值对）
     */
    private Map<String, String> pathParams;

    /**
     * 查询参数（键值对）
     */
    private Map<String, String> queryParams;

    /**
     * 请求头（键值对）
     */
    private Map<String, String> headers;

    /**
     * 请求体（JSON 字符串）
     */
    private String body;
}
