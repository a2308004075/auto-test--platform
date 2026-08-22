/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description API 信息响应 DTO
 */
package com.platform.apidoc.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 接口文档响应 DTO
 */
@Data
public class ApiInfoResponse {

    private Long id;
    private Long projectId;
    private Long moduleId;
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
