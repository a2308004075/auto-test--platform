package com.platform.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 启用/禁用用户请求
 */
@Data
public class StatusToggleRequest {

    @NotNull(message = "isActive 不能为空")
    private Boolean isActive;
}
