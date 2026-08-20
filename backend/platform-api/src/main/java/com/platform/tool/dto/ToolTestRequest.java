package com.platform.tool.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class ToolTestRequest {

    @NotBlank(message = "测试输入不能为空")
    private String testInput;
}
