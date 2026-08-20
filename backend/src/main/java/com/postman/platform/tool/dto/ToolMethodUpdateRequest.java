package com.postman.platform.tool.dto;

import lombok.Data;

import javax.validation.constraints.Size;

@Data
public class ToolMethodUpdateRequest {

    @Size(max = 100, message = "工具名称长度不能超过 100")
    private String name;

    @Size(max = 500, message = "描述长度不能超过 500")
    private String description;

    private String paramDefinitions;
    private String returnType;
    private String code;
}
