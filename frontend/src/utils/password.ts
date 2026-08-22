/**
 * @author HXN
 * @date 2026-08-22 12:41
 * @description 密码工具函数
 */
/**
 * 密码策略工具
 *
 * 规则：长度 6-32 位，必须同时包含大写字母、小写字母、数字和英文符号。
 * 与后端 PasswordPolicy 常量保持一致。
 */

export const PASSWORD_MIN_LENGTH = 6
export const PASSWORD_MAX_LENGTH = 32

/** 密码规则简述，用于表单占位文本 */
export const PASSWORD_RULE_HINT = '6-32位，需包含大写字母、小写字母、数字和英文符号'

/**
 * 校验密码格式
 * @param password 待校验的密码明文
 * @returns 校验通过返回空字符串，否则返回错误提示
 */
export function validatePassword(password: string): string {
  if (!password) {
    return '请输入密码'
  }
  if (password.length < PASSWORD_MIN_LENGTH) {
    return `密码长度不能少于${PASSWORD_MIN_LENGTH}位`
  }
  if (password.length > PASSWORD_MAX_LENGTH) {
    return `密码长度不能超过${PASSWORD_MAX_LENGTH}位`
  }
  if (!/[a-z]/.test(password)) {
    return '密码必须包含小写字母'
  }
  if (!/[A-Z]/.test(password)) {
    return '密码必须包含大写字母'
  }
  if (!/\d/.test(password)) {
    return '密码必须包含数字'
  }
  if (!/[^a-zA-Z0-9]/.test(password)) {
    return '密码必须包含英文符号'
  }
  return ''
}
