/**
 * @author HXN
 * @date 2026-08-22 12:41
 * @description 密码策略常量类
 */
package com.platform.common.constant;

/**
 * 密码策略常量
 *
 * <p>统一定义密码长度与组成规则，供后端 DTO 校验注解和 Service 层引用。
 * 前端在 {@code utils/password.ts} 中维护等价规则。
 */
public final class PasswordPolicy {

    private PasswordPolicy() {
    }

    /** 密码最小长度 */
    public static final int MIN_LENGTH = 6;

    /** 密码最大长度 */
    public static final int MAX_LENGTH = 32;

    /**
     * 密码组成正则：必须同时包含大写字母、小写字母、数字和英文符号。
     *
     * <p>四个正向预查分别确保：
     * <ul>
     *   <li>{@code (?=.*[a-z])} — 至少一个小写字母</li>
     *   <li>{@code (?=.*[A-Z])} — 至少一个大写字母</li>
     *   <li>{@code (?=.*\d)}   — 至少一个数字</li>
     *   <li>{@code (?=.*[^a-zA-Z0-9])} — 至少一个英文符号</li>
     * </ul>
     * 尾部 {@code .+} 匹配实际内容，长度由 {@code @Size} 独立约束。
     */
    public static final String PATTERN =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).+$";

    /** 密码组成校验失败消息 */
    public static final String PATTERN_MESSAGE =
            "密码必须包含大写字母、小写字母、数字和英文符号";

    /** 密码长度校验失败消息 */
    public static final String SIZE_MESSAGE =
            "密码长度必须在 6-32 之间";
}
