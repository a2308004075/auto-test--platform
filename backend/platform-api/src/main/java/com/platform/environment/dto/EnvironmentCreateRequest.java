/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 环境创建请求 DTO
 */
package com.platform.environment.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 创建环境请求
 */
@Data
public class EnvironmentCreateRequest {

    private Long projectId;

    @NotBlank(message = "环境名称不能为空")
    @Size(max = 100, message = "环境名称长度不能超过 100")
    private String name;

    @Size(max = 255, message = "环境描述长度不能超过 255")
    private String description;
}
