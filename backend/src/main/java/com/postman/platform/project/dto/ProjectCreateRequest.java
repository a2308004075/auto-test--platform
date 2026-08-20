package com.postman.platform.project.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 创建项目请求
 */
@Data
public class ProjectCreateRequest {

    @NotBlank(message = "项目名称不能为空")
    @Size(max = 50, message = "项目名称长度不能超过 50")
    private String name;

    @Size(max = 500, message = "项目描述长度不能超过 500")
    private String description;

    @Size(max = 500, message = "源码路径长度不能超过 500")
    private String sourcePath;
}
