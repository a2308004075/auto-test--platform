package com.postman.platform.keyword.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 创建接口关键字请求
 */
@Data
public class ApiKeywordCreateRequest {

    @NotNull(message = "项目 ID 不能为空")
    private Long projectId;

    @NotBlank(message = "关键字名称不能为空")
    @Size(max = 100, message = "关键字名称长度不能超过 100")
    private String name;

    @NotNull(message = "接口 ID 不能为空")
    private Long apiId;

    @Size(max = 500, message = "描述长度不能超过 500")
    private String description;

    /**
     * 测试数据（JSON 格式）
     */
    private String testData;

    /**
     * 响应断言配置（JSON 格式）
     */
    private String responseAssertion;
}
