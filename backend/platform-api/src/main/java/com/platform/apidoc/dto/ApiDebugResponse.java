/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description API 调试响应 DTO
 */
package com.platform.apidoc.dto;

import lombok.Data;

import java.util.Map;

/**
 * 接口调试响应
 */
@Data
public class ApiDebugResponse {

    private Integer statusCode;
    private String statusText;
    private Map<String, String> responseHeaders;
    private String responseBody;
    private Long responseTimeMs;
    private Long responseSizeBytes;
    private Integer success;
    private String errorMessage;

    public static ApiDebugResponse error(String errorMessage) {
        ApiDebugResponse r = new ApiDebugResponse();
        r.setSuccess(0);
        r.setErrorMessage(errorMessage);
        return r;
    }
}
