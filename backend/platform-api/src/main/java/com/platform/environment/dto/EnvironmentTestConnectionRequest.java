/**
 * @author HXN
 * @date 2026-08-23
 * @description 环境测试连接请求 DTO
 */
package com.platform.environment.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 测试目标服务连通性请求
 */
@Data
public class EnvironmentTestConnectionRequest {

    @NotBlank(message = "目标地址不能为空")
    private String url;

    /**
     * 请求方法，默认 GET
     */
    private String method = "GET";
}
