/**
 * @author HXN
 * @date 2026-08-22 13:27
 * @description 角色创建请求 DTO
 */
package com.platform.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * 角色创建/编辑请求
 */
@Data
public class RoleCreateRequest {

    /**
     * 角色名称（显示名）
     */
    @NotBlank(message = "角色名称不能为空")
    private String roleName;

    /**
     * 角色编码（如 ADMIN、DEVELOPER）
     */
    @NotBlank(message = "角色编码不能为空")
    private String roleCode;

    /**
     * 角色描述
     */
    private String description;

    /**
     * 排序号
     */
    private Integer sortOrder;

    /**
     * 权限 ID 列表（创建/编辑时一并分配）
     */
    private List<Long> permissionIds;
}
