package com.platform.auth.dto;

import lombok.Data;

/**
 * 验证码响应
 */
@Data
public class CaptchaResponse {

    /**
     * 验证码 ID（用于登录时提交校验）
     */
    private String captchaId;

    /**
     * Base64 编码的验证码图片
     */
    private String image;
}
