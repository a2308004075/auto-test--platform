/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description API 调试请求 DTO
 */
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

    /**
     * 请求体格式：none/form_data/x_www_form_urlencoded/raw/binary/graphql
     */
    private String bodyType;

    /**
     * raw 子类型：text/javascript/json/html/xml
     */
    private String rawType;
}
