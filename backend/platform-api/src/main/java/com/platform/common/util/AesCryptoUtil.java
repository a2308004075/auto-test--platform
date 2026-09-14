/**
 * @author HXN
 * @date 2026-08-30 10:00
 * @description AES 加解密工具类
 */
package com.platform.common.util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES 加解密工具
 *
 * <p>算法：AES-128/CBC/PKCS5Padding。每次加密使用随机 IV，IV 前置拼接密文后整体 Base64，
 * 密文以 {@code enc:} 前缀标识；解密时兼容无前缀的历史明文值（直接原样返回）。
 */
public final class AesCryptoUtil {

    /**
     * 密文前缀标识
     */
    private static final String CIPHER_PREFIX = "enc:";

    /**
     * 算法/模式/填充
     */
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";

    /**
     * 密钥算法
     */
    private static final String KEY_ALGORITHM = "AES";

    /**
     * AES-128 密钥字节长度
     */
    private static final int KEY_LENGTH = 16;

    /**
     * IV 字节长度
     */
    private static final int IV_LENGTH = 16;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private AesCryptoUtil() {
    }

    /**
     * 加密明文
     *
     * @param plain 明文
     * @param key   密钥（16 字节字符串）
     * @return 带 enc: 前缀的密文
     */
    public static String encrypt(String plain, String key) {
        if (plain == null || plain.isEmpty()) {
            return plain;
        }
        try {
            byte[] keyBytes = normalizeKey(key);
            byte[] iv = new byte[IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, KEY_ALGORITHM), new IvParameterSpec(iv));
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));

            // IV 前置拼接后整体 Base64
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return CIPHER_PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("AES 加密失败", e);
        }
    }

    /**
     * 解密密文
     *
     * @param cipherText 带 enc: 前缀的密文；无前缀的值视为历史明文直接返回
     * @param key        密钥（16 字节字符串）
     * @return 明文
     */
    public static String decrypt(String cipherText, String key) {
        if (cipherText == null || cipherText.isEmpty()) {
            return cipherText;
        }
        if (!cipherText.startsWith(CIPHER_PREFIX)) {
            return cipherText;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(cipherText.substring(CIPHER_PREFIX.length()));
            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(normalizeKey(key), KEY_ALGORITHM), new IvParameterSpec(iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("AES 解密失败", e);
        }
    }

    /**
     * 密钥规范化：超长截断、不足补零，保证 16 字节
     */
    private static byte[] normalizeKey(String key) {
        byte[] source = key.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[KEY_LENGTH];
        int length = Math.min(source.length, KEY_LENGTH);
        System.arraycopy(source, 0, result, 0, length);
        return result;
    }
}
