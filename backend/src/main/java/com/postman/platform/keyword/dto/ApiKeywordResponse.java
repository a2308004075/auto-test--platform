package com.postman.platform.keyword.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 接口关键字响应
 */
@Data
public class ApiKeywordResponse {

    private String id;
    private String keywordId;
    private String projectId;
    private String name;
    private String type;
    private String apiId;
    private String testData;
    private String responseAssertion;
    private String description;
    private String createdBy;
    private String updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 关联的接口名称（用于前端展示）
     */
    private String apiName;

    /**
     * 引用次数（被 Action / 用例引用的次数）
     */
    private Integer referenceCount;
}
