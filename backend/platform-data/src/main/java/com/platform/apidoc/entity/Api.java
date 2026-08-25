/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description API 接口实体类
 */
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

    /**
     * 默认 Content-Type（如 application/json、application/x-www-form-urlencoded）
     */
    private String contentType;

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
