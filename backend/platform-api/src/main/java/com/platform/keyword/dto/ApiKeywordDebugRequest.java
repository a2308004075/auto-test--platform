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
 * <p>testData 有值时以其作为请求参数执行（调试覆盖），否则使用关键字已保存的 testData。</p>
 */
@Data
public class ApiKeywordDebugRequest {

    @NotNull(message = "环境 ID 不能为空")
    private Long environmentId;

    /**
     * 测试数据覆盖（JSON 数组，name/value 结构，可选）
     */
    private String testData;
}
