package com.platform.keyword.dto;

import lombok.Data;

import javax.validation.constraints.Size;

/**
 * 更新接口关键字请求
 */
@Data
public class ApiKeywordUpdateRequest {

    @Size(max = 100, message = "关键字名称长度不能超过 100")
    private String name;

    @Size(max = 500, message = "描述长度不能超过 500")
    private String description;

    private String testData;

    private String responseAssertion;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签列表（JSON 数组）
     */
    private String tags;
}
