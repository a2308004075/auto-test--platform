/**
 * @author HXN
 * @date 2026-08-30
 * @description 缺陷分组更新请求 DTO
 */
package com.platform.execution.dto;

import lombok.Data;

import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 缺陷分组更新请求
 */
@Data
public class DefectGroupUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Size(max = 100, message = "分组名称长度不能超过 100")
    private String name;

    @Size(max = 500, message = "分组描述长度不能超过 500")
    private String description;

    private Long parentId;
}
