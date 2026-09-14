/**
 * @author HXN
 * @date 2026-08-22 13:28
 * @description 权限同步结果 DTO
 */
package com.platform.auth.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 权限同步结果
 *
 * <p>记录从 sys_menu 同步到 permission 表的结果统计。
 */
@Data
public class PermissionSyncResult {

    /**
     * 新增的权限数量
     */
    private int createdCount;

    /**
     * 更新的权限数量
     */
    private int updatedCount;

    /**
     * 跳过的权限数量（sys_menu 中未设置 permission_code 的条目）
     */
    private int skippedCount;

    /**
     * 新增的权限名称列表
     */
    private List<String> createdNames = new ArrayList<>();

    /**
     * 更新的权限名称列表
     */
    private List<String> updatedNames = new ArrayList<>();
}
