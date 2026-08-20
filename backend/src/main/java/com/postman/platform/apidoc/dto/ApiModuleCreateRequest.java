package com.postman.platform.apidoc.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 创建接口分组请求
 */
@Data
public class ApiModuleCreateRequest {

    @NotNull(message = "项目 ID 不能为空")
    private Long projectId;

    @NotBlank(message = "分组名称不能为空")
    @Size(max = 100, message = "分组名称长度不能超过 100")
    private String name;

    @Size(max = 50, message = "服务前缀长度不能超过 50")
    private String servicePrefix;

    @Size(max = 500, message = "描述长度不能超过 500")
    private String description;
}
