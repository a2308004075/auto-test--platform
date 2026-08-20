package com.postman.platform.tool.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class ToolMethodCreateRequest {

    @NotBlank(message = "项目 ID 不能为空")
    private String projectId;

    @NotBlank(message = "工具名称不能为空")
    @Size(max = 100, message = "工具名称长度不能超过 100")
    private String name;

    private String category;

    @Size(max = 500, message = "描述长度不能超过 500")
    private String description;

    private String paramDefinitions;

    private String returnType;

    @NotBlank(message = "Groovy 代码不能为空")
    private String code;
}
