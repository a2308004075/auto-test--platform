/**
 * @author HXN
 * @date 2026-08-24
 * @description Swagger 同步配置请求 DTO
 */
package com.platform.apidoc.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class ApiSyncConfigRequest {

    @NotBlank(message = "配置名称不能为空")
    private String name;

    @NotBlank(message = "URL 不能为空")
    private String url;

    @NotNull(message = "目标分组不能为空")
    private Long moduleId;

    /** 自定义请求头文本（多行 Key: Value） */
    private String headers;

    /** 认证账号（Basic Auth，可选） */
    private String authUsername;

    /** 认证密码（Basic Auth，可选） */
    private String authPassword;
}
