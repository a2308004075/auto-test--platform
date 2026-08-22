/**
 * @author HXN
 * @date 2026-08-20 19:14
 * @description 验证码服务
 */
package com.platform.auth.service;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.util.IdUtil;
import com.platform.auth.dto.CaptchaResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 验证码服务 - 生成图片验证码并通过 Redis 存储校验
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private static final String CAPTCHA_KEY_PREFIX = "captcha:";
    private static final long CAPTCHA_EXPIRE_MINUTES = 5;

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 生成验证码
     *
     * <p>流程：创建 LineCaptcha → 生成 UUID 作为 captchaId → 存入 Redis（5分钟过期）→ 返回 captchaId + base64 图片
     *
     * @return 验证码响应（captchaId + base64 图片）
     */
    public CaptchaResponse generateCaptcha() {
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(200, 50, 4, 30);
        String code = captcha.getCode();
        String captchaId = IdUtil.fastSimpleUUID();

        stringRedisTemplate.opsForValue().set(
                CAPTCHA_KEY_PREFIX + captchaId,
                code,
                CAPTCHA_EXPIRE_MINUTES,
                TimeUnit.MINUTES
        );

        CaptchaResponse response = new CaptchaResponse();
        response.setCaptchaId(captchaId);
        response.setImage(captcha.getImageBase64Data());
        return response;
    }

    /**
     * 校验验证码
     *
     * <p>流程：从 Redis 读取验证码 → 比对（忽略大小写）→ 删除 Redis 中的记录（一次性使用）
     *
     * @param captchaId   验证码 ID
     * @param captchaCode 用户输入的验证码
     * @return true 验证通过，false 验证失败
     */
    public boolean verifyCaptcha(String captchaId, String captchaCode) {
        if (captchaId == null || captchaCode == null || captchaCode.isEmpty()) {
            return false;
        }

        String redisKey = CAPTCHA_KEY_PREFIX + captchaId;
        String storedCode = stringRedisTemplate.opsForValue().get(redisKey);

        // 无论校验是否通过，都删除验证码（一次性使用）
        stringRedisTemplate.delete(redisKey);

        if (storedCode == null) {
            return false;
        }

        return storedCode.equalsIgnoreCase(captchaCode);
    }
}
