/**
 * @author HXN
 * @date 2026-09-14 10:00
 * @description 远程仓库分支列表响应 DTO
 */
package com.platform.repository.dto;

import lombok.Data;

import java.util.List;

/**
 * 远程仓库分支列表响应
 */
@Data
public class RepositoryBranchListResponse {

    /**
     * 远程分支名列表（已按名称排序）
     */
    private List<String> branches;

    /**
     * 仓库默认分支（HEAD 指向的分支，服务器未通告时为 null）
     */
    private String defaultBranch;
}
