/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 状态切换请求 DTO
 */
package com.platform.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 启用/禁用用户请求
 */
@Data
public class StatusToggleRequest {

    @NotNull(message = "isActive 不能为空")
    private Integer isActive;
}
