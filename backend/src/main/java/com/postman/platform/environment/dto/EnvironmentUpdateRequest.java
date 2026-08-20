package com.postman.platform.environment.dto;

import lombok.Data;

import javax.validation.constraints.Size;

/**
 * 更新环境请求
 */
@Data
public class EnvironmentUpdateRequest {

    @Size(max = 100, message = "环境名称长度不能超过 100")
    private String name;

    @Size(max = 200, message = "主机地址长度不能超过 200")
    private String host;

    private Integer port;

    @Size(max = 100, message = "数据库名长度不能超过 100")
    private String databaseName;

    @Size(max = 100, message = "用户名长度不能超过 100")
    private String username;

    @Size(max = 200, message = "密码长度不能超过 200")
    private String password;

    private String configJson;
}
