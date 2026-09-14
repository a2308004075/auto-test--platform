/**
 * @author HXN
 * @date 2026-09-14 10:00
 * @description 远程仓库分支列表查询请求 DTO
 */
package com.platform.repository.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 远程仓库分支列表查询请求
 *
 * <p>编辑场景 authPassword 留空时，可传 repositoryId 由后端使用已保存凭证；
 * 两者都未提供时匿名访问（公开仓库）。
 */
@Data
public class RepositoryBranchListRequest {

    @NotBlank(message = "Git 地址不能为空")
    @Size(max = 500, message = "Git 地址长度不能超过 500")
    private String gitUrl;

    @Size(max = 200, message = "认证用户名长度不能超过 200")
    private String authUsername;

    @Size(max = 500, message = "认证密码长度不能超过 500")
    private String authPassword;

    /**
     * 编辑仓库 ID（可选）：authPassword 留空时使用该仓库已保存的凭证
     */
    private Long repositoryId;
}
