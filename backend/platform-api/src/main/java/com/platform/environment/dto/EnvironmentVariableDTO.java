/**
 * @author HXN
 * @date 2026-08-23
 * @description 环境变量 DTO
 */
package com.platform.environment.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 环境变量 DTO（键值对）
 */
@Data
public class EnvironmentVariableDTO {

    @NotBlank(message = "变量名不能为空")
    @Size(max = 100, message = "变量名长度不能超过 100")
    private String varKey;

    @Size(max = 2000, message = "变量值长度不能超过 2000")
    private String varValue;

    @Size(max = 500, message = "变量描述长度不能超过 500")
    private String description;
}
