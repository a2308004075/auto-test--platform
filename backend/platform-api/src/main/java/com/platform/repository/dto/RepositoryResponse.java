/**
 * @author HXN
 * @date 2026-08-30 10:00
 * @description 代码仓库响应 DTO
 */
package com.platform.repository.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 代码仓库响应
 *
 * <p>认证密码不回传，仅返回 hasAuth 标识是否已配置认证。
 */
@Data
public class RepositoryResponse {

    private Long id;

    private Long projectId;

    private String name;

    private String gitUrl;

    private String branch;

    private String description;

    private String authUsername;

    /**
     * 是否已配置认证密码/Token
     */
    private Boolean hasAuth;

    private String localPath;

    private LocalDateTime lastPullAt;

    private String lastPullStatus;

    private String lastCommitId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
