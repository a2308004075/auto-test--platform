package com.platform.keyword.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 接口关键字响应
 */
@Data
public class ApiKeywordResponse {

    private Long id;
    private Long keywordId;
    private Long projectId;
    private String name;
    private String type;
    private Long apiId;
    private String testData;
    private String responseAssertion;
    private String category;
    private String tags;
    private String description;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 关联的接口名称（用于前端展示）
     */
    private String apiName;

    /**
     * 关联接口路径
     */
    private String apiPath;

    /**
     * 关联接口 HTTP 方法
     */
    private String httpMethod;

    /**
     * 关联接口所属分组 ID
     */
    private Long moduleId;

    /**
     * 关联接口所属分组名称
     */
    private String moduleName;

    /**
     * 引用次数（被 Action / 用例引用的次数）
     */
    private Integer referenceCount;
}
