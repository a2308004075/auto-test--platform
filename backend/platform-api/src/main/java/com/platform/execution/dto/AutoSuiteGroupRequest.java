/**
 * @author HXN
 * @date 2026-08-23 10:00
 * @description 自动化套件分组创建/更新请求 DTO
 */
package com.platform.execution.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 自动化套件分组创建/更新请求
 */
@Data
public class AutoSuiteGroupRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "分组名称不能为空")
    @Size(max = 50, message = "分组名称长度不能超过 50")
    private String name;

    @Size(max = 500, message = "描述长度不能超过 500")
    private String description;

    /**
     * 父分组 ID（null 表示顶层分组）
     */
    private Long parentId;

    /**
     * 排序号
     */
    private Integer sortNo;
}
