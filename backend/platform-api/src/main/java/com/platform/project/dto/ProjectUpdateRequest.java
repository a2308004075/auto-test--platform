package com.platform.project.dto;

import lombok.Data;

import javax.validation.constraints.Size;

/**
 * 更新项目请求
 */
@Data
public class ProjectUpdateRequest {

    @Size(max = 50, message = "项目名称长度不能超过 50")
    private String name;

    @Size(max = 500, message = "项目描述长度不能超过 500")
    private String description;

    @Size(max = 500, message = "源码路径长度不能超过 500")
    private String sourcePath;
}
