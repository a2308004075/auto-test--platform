package com.platform.common.response;

import lombok.Data;

/**
 * 统一 API 响应格式
 *
 * @param <T> 响应数据类型
 */
@Data
public class ApiResponse<T> {

    private int code;
    private String message;
    private T data;

    public ApiResponse() {
    }

    public ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "success", data);
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(0, message, data);
    }

    /**
     * 便捷方法：成功响应（无数据）
     */
    public static <T> ApiResponse<T> ok() {
        return new ApiResponse<>(0, "success", null);
    }

    /**
     * 便捷方法：成功响应（带数据）
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "success", data);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    public static <T> ApiResponse<T> error(int code, String message, T data) {
        return new ApiResponse<>(code, message, data);
    }
}
