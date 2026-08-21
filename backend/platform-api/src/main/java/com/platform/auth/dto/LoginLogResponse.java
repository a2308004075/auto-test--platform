package com.platform.auth.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志响应
 */
@Data
public class LoginLogResponse {

    private Long id;
    /** 登录状态：SUCCESS / FAILED */
    private String status;
    /** 客户端 IP */
    private String ip;
    /** 浏览器 */
    private String browser;
    /** 操作系统 */
    private String os;
    /** 附加信息（如失败原因） */
    private String message;
    /** 登录时间 */
    private LocalDateTime createdAt;
}
