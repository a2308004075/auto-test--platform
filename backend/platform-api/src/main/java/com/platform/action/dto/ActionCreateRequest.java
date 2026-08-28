/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description Action 创建请求 DTO
 */
package com.platform.action.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;

@Data
public class ActionCreateRequest {

    private Long projectId;

    @NotBlank(message = "Action 名称不能为空")
    @Size(max = 100, message = "Action 名称长度不能超过 100")
    private String name;

    @Size(max = 500, message = "描述长度不能超过 500")
    private String description;

    private Long groupId;

    private List<ActionNodeDTO> nodes;

    private String inputParams;

    private String outputParams;
}
