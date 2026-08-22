/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description Action 调试请求 DTO
 */
package com.platform.action.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Map;

@Data
public class ActionDebugRequest {

    @NotNull(message = "环境 ID 不能为空")
    private Long environmentId;

    /**
     * 输入参数（键值对）
     */
    private Map<String, Object> inputParams;
}
