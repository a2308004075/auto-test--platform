package com.platform.common.exception;

import lombok.Getter;

/**
 * 业务异常 - 所有可预期的业务错误均抛出此异常
 *
 * <p>使用示例：
 * <pre>
 *   throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "项目不存在");
 * </pre>
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
