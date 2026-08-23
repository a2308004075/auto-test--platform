/**
 * @author HXN
 * @date 2026-08-23
 * @description 环境测试连接响应 DTO
 */
package com.platform.environment.dto;

import lombok.Data;

/**
 * 测试目标服务连通性响应
 */
@Data
public class EnvironmentTestConnectionResponse {

    private boolean success;

    private int statusCode;

    private long durationMs;

    private String message;

    public static EnvironmentTestConnectionResponse success(int statusCode, long durationMs) {
        EnvironmentTestConnectionResponse resp = new EnvironmentTestConnectionResponse();
        resp.setSuccess(true);
        resp.setStatusCode(statusCode);
        resp.setDurationMs(durationMs);
        resp.setMessage("连接成功");
        return resp;
    }

    public static EnvironmentTestConnectionResponse error(String message) {
        EnvironmentTestConnectionResponse resp = new EnvironmentTestConnectionResponse();
        resp.setSuccess(false);
        resp.setMessage(message);
        return resp;
    }
}
