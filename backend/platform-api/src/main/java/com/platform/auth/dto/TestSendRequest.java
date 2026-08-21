package com.platform.auth.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 测试发送请求（SMTP 邮件 / Webhook 通知）
 */
@Data
public class TestSendRequest {

    /**
     * 接收地址（SMTP 为收件人邮箱，Webhook 为回调地址，可选）
     */
    private String recipient;

    /**
     * 通知内容
     */
    @NotBlank(message = "通知内容不能为空")
    private String content;
}
