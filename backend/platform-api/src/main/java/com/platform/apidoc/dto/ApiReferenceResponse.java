/**
 * @author HXN
 * @date 2026-08-21 15:30
 * @description API 引用响应 DTO
 */
package com.platform.apidoc.dto;

import lombok.Data;

/**
 * 接口引用关系响应
 */
@Data
public class ApiReferenceResponse {

    /**
     * 关键字 ID
     */
    private Long keywordId;

    /**
     * 关键字名称
     */
    private String keywordName;

    /**
     * 关键字类型
     */
    private String keywordType;

    /**
     * 关键字分类
     */
    private String category;

    /**
     * 引用次数（被 Action / 用例引用）
     */
    private Integer referenceCount;
}
