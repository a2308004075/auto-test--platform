package com.platform.keyword.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.platform.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 接口关键字绑定（对应 api_keyword 表）
 * 将关键字与接口绑定，并存储测试数据和响应断言
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("api_keyword")
public class ApiKeyword extends BaseEntity {

    private Long keywordId;

    private Long projectId;

    private Long apiId;

    /**
     * 测试数据（JSON 格式）
     */
    private String testData;

    /**
     * 响应断言配置（JSON 格式）
     */
    private String responseAssertion;
}
