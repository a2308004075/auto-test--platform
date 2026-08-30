/**
 * @author HXN
 * @date 2026-08-30 10:00
 * @description 代码仓库创建请求 DTO
 */
package com.platform.repository.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 创建代码仓库请求
 */
@Data
public class RepositoryCreateRequest {

    private Long projectId;

    @NotBlank(message = "仓库名称不能为空")
    @Size(max = 50, message = "仓库名称长度不能超过 50")
    private String name;

    @NotBlank(message = "Git 地址不能为空")
    @Size(max = 500, message = "Git 地址长度不能超过 500")
    private String gitUrl;

    @Size(max = 100, message = "分支长度不能超过 100")
    private String branch;

    @Size(max = 255, message = "仓库描述长度不能超过 255")
    private String description;

    @Size(max = 200, message = "认证用户名长度不能超过 200")
    private String authUsername;

    @Size(max = 500, message = "认证密码长度不能超过 500")
    private String authPassword;
}
