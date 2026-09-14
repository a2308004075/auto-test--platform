/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 环境更新请求 DTO
 */
package com.platform.environment.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 更新环境请求
 */
@Data
public class EnvironmentUpdateRequest {

    @Size(max = 100, message = "环境名称长度不能超过 100")
    private String name;

    @Size(max = 255, message = "环境描述长度不能超过 255")
    private String description;

    /**
     * 环境变量列表（全量替换）
     */
    @Valid
    private List<EnvironmentVariableDTO> variables;
}
