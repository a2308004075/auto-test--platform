package com.postman.platform.apidoc.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 接口文档响应 DTO
 */
@Data
public class ApiInfoResponse {

    private String id;
    private String projectId;
    private String moduleId;
    private String name;
    private String service;
    private String httpMethod;
    private String path;
    private String requestParams;
    private String requestBody;
    private String responseBody;
    private String headers;
    private String description;
    private String sourceType;
    private String swaggerOperationId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
