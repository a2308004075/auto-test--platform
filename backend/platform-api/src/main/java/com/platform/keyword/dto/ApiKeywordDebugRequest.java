/**
 * @author HXN
 * @date 2026-08-23
 * @description 接口关键字调试请求 DTO
 */
package com.platform.keyword.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 接口关键字在线调试请求
 * <p>仅传入环境 ID，请求参数按关键字保存的 testData 执行。</p>
 */
@Data
public class ApiKeywordDebugRequest {

    @NotNull(message = "环境 ID 不能为空")
    private Long environmentId;
}
