import request from './request'

/**
 * 认证模块 API
 */

// 获取验证码图片
export function getCaptcha() {
  return request.get('/v1/auth/captcha')
}

// 登录
export function login(data: { username: string; password: string; captchaId: string; captchaCode: string }) {
  return request.post('/v1/auth/login', data)
}

// 获取当前用户信息
export function getCurrentUser() {
  return request.get('/v1/auth/me')
}

// 刷新 Token
export function refreshToken(refreshToken: string) {
  return request.post('/v1/auth/refresh', { refreshToken })
}

// 退出登录
export function logout() {
  return request.post('/v1/auth/logout')
}
