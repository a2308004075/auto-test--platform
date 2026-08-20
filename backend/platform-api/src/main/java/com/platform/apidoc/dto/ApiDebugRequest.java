package com.platform.apidoc.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * 接口调试请求
 */
@Data
public class ApiDebugRequest {

    @NotNull(message = "环境 ID 不能为空")
    private Long environmentId;

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
