/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description API 模块更新请求 DTO
 */
package com.platform.apidoc.dto;

import lombok.Data;

import javax.validation.constraints.Size;

/**
 * 更新接口分组请求
 */
@Data
public class ApiModuleUpdateRequest {

    /**
     * 父分组 ID（null=根分组）
     */
    private Long parentId;

    @Size(max = 100, message = "分组名称长度不能超过 100")
    private String name;

    @Size(max = 50, message = "服务前缀长度不能超过 50")
    private String servicePrefix;

    @Size(max = 500, message = "描述长度不能超过 500")
    private String description;
}
