package com.postman.platform.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 全局配置更新请求
 */
@Data
public class GlobalConfigUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 配置值
     */
    @NotBlank(message = "配置值不能为空")
    @Size(max = 2000, message = "配置值长度不能超过 2000")
    private String configValue;

    /**
     * 配置说明（可选）
     */
    private String description;
}
