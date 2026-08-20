package com.postman.platform.action.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.util.Map;

@Data
public class ActionDebugRequest {

    @NotBlank(message = "环境 ID 不能为空")
    private String environmentId;

    /**
     * 输入参数（键值对）
     */
    private Map<String, Object> inputParams;
}
