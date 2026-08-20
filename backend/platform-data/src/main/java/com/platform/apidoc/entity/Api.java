package com.platform.apidoc.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 接口定义实体（对应 api 表）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("api")
public class Api extends BaseEntity {

    private Long projectId;

    private Long moduleId;

    private String name;

    private String service;

    /**
     * HTTP 方法（GET/POST/PUT/DELETE/PATCH）
     */
    private String httpMethod;

    private String path;

    /**
     * 请求参数（JSON 数组）
     */
    private String requestParams;

    /**
     * 请求体 Schema（JSON）
     */
    private String requestBody;

    /**
     * 响应体 Schema（JSON）
     */
    private String responseBody;

    /**
     * 请求头（JSON 数组）
     */
    private String headers;

    private String description;

    /**
     * 来源类型：MANUAL / SWAGGER_IMPORT
     */
    private String sourceType;

    /**
     * Swagger 操作 ID（用于增量同步）
     */
    private String swaggerOperationId;
}
