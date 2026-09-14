/**
 * @author HXN
 * @date 2026-08-20 19:14
 * @description 验证码服务
 */
package com.platform.auth.service;

import cn.hutool.core.util.IdUtil;
import com.platform.auth.dto.CaptchaResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Random;
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

    /** 验证码图片宽度 */
    private static final int CAPTCHA_WIDTH = 140;
    /** 验证码图片高度 */
    private static final int CAPTCHA_HEIGHT = 50;
    /** 验证码字符数量 */
    private static final int CAPTCHA_COUNT = 4;
    /** 验证码字符集（排除易混淆字符 0/O、1/I/l） */
    private static final String CAPTCHA_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    /** 背景色 - 浅灰，与参考样式一致 */
    private static final Color BACKGROUND_COLOR = new Color(230, 230, 230);
    /** 字符色 - 蓝色，与参考样式一致 */
    private static final Color TEXT_COLOR = new Color(24, 144, 255);
    /** 干扰线/噪点色 - 中灰 */
    private static final Color NOISE_COLOR = new Color(150, 150, 150);
    /** 验证码字体 */
    private static final Font CAPTCHA_FONT = new Font("Arial", Font.BOLD, 32);

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 生成验证码
     *
     * <p>流程：绘制验证码图片 → 生成 UUID 作为 captchaId → 存入 Redis（5分钟过期）→ 返回 captchaId + base64 图片
     *
     * @return 验证码响应（captchaId + base64 图片）
     */
    public CaptchaResponse generateCaptcha() {
        String code = generateCaptchaCode();
        String imageBase64 = drawCaptchaImage(code);
        String captchaId = IdUtil.fastSimpleUUID();

        stringRedisTemplate.opsForValue().set(
                CAPTCHA_KEY_PREFIX + captchaId,
                code,
                CAPTCHA_EXPIRE_MINUTES,
                TimeUnit.MINUTES
        );

        CaptchaResponse response = new CaptchaResponse();
        response.setCaptchaId(captchaId);
        response.setImage(imageBase64);
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

    /**
     * 随机生成验证码字符串
     *
     * @return 验证码字符串
     */
    private String generateCaptchaCode() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(CAPTCHA_COUNT);
        for (int i = 0; i < CAPTCHA_COUNT; i++) {
            sb.append(CAPTCHA_CHARS.charAt(random.nextInt(CAPTCHA_CHARS.length())));
        }
        return sb.toString();
    }

    /**
     * 绘制验证码图片
     *
     * <p>样式说明：浅灰背景、蓝色字符、带干扰斜线与噪点，字符做随机旋转与垂直偏移
     *
     * @param code 验证码字符串
     * @return base64 编码的图片（含 data:image/png;base64, 前缀）
     */
    private String drawCaptchaImage(String code) {
        BufferedImage image = new BufferedImage(CAPTCHA_WIDTH, CAPTCHA_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // 抗锯齿
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 背景填充
        g.setColor(BACKGROUND_COLOR);
        g.fillRect(0, 0, CAPTCHA_WIDTH, CAPTCHA_HEIGHT);

        Random random = new Random();

        // 绘制干扰斜线
        g.setColor(NOISE_COLOR);
        for (int i = 0; i < 4; i++) {
            int x1 = random.nextInt(CAPTCHA_WIDTH);
            int y1 = random.nextInt(CAPTCHA_HEIGHT);
            int x2 = random.nextInt(CAPTCHA_WIDTH);
            int y2 = random.nextInt(CAPTCHA_HEIGHT);
            g.drawLine(x1, y1, x2, y2);
        }

        // 绘制验证码字符
        g.setFont(CAPTCHA_FONT);
        g.setColor(TEXT_COLOR);
        FontMetrics fm = g.getFontMetrics();
        int cellWidth = CAPTCHA_WIDTH / CAPTCHA_COUNT;
        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);
            int charWidth = fm.charWidth(c);
            int charX = i * cellWidth + (cellWidth - charWidth) / 2;
            int charY = (CAPTCHA_HEIGHT + fm.getAscent() - fm.getDescent()) / 2 + random.nextInt(8) - 4;

            AffineTransform origin = g.getTransform();
            AffineTransform transform = new AffineTransform();
            // 随机旋转 -15° ~ 15°
            double theta = (random.nextDouble() - 0.5) * Math.PI / 6;
            transform.rotate(theta, charX + charWidth / 2.0, charY);
            g.setTransform(transform);
            g.drawString(String.valueOf(c), charX, charY);
            g.setTransform(origin);
        }

        // 绘制噪点
        for (int i = 0; i < 40; i++) {
            int x = random.nextInt(CAPTCHA_WIDTH);
            int y = random.nextInt(CAPTCHA_HEIGHT);
            image.setRGB(x, y, NOISE_COLOR.getRGB());
        }

        g.dispose();
        return encodeImageToBase64(image);
    }

    /**
     * 将 BufferedImage 编码为 base64 字符串
     *
     * @param image 验证码图片
     * @return base64 编码字符串（含 data:image/png;base64, 前缀）
     */
    private String encodeImageToBase64(BufferedImage image) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", outputStream);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (IOException e) {
            log.error("验证码图片 base64 编码失败", e);
            throw new RuntimeException("验证码图片生成失败", e);
        }
    }
}
