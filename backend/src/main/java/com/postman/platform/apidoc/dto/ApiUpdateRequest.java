package com.postman.platform.apidoc.dto;

import lombok.Data;

import javax.validation.constraints.Size;

/**
 * 更新接口请求
 */
@Data
public class ApiUpdateRequest {

    private String moduleId;

    @Size(max = 200, message = "接口名称长度不能超过 200")
    private String name;

    @Size(max = 100, message = "服务名长度不能超过 100")
    private String service;

    private String httpMethod;

    @Size(max = 500, message = "路径长度不能超过 500")
    private String path;

    private String requestParams;
    private String requestBody;
    private String responseBody;
    private String headers;

    @Size(max = 2000, message = "描述长度不能超过 2000")
    private String description;
}
