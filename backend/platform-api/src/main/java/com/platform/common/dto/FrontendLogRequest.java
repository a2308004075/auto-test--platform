/**
 * @author HXN
 * @date 2026-08-30
 * @description 前端错误日志上报请求体
 */
package com.platform.common.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 前端错误日志上报请求体
 */
@Data
public class FrontendLogRequest {

    /**
     * 错误类型：vue_error / js_error / promise_rejection / api_error
     */
    @NotBlank(message = "错误类型不能为空")
    private String type;

    /**
     * 错误消息
     */
    @NotBlank(message = "错误消息不能为空")
    private String message;

    /**
     * 错误堆栈（可选）
     */
    private String stack;

    /**
     * 发生错误的页面 URL
     */
    private String url;

    /**
     * 用户代理信息
     */
    private String userAgent;

    /**
     * 附加上下文信息（如 API 请求地址、状态码等）
     */
    private String extra;
}
