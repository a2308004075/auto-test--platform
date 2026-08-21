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

// 更新个人资料
export function updateProfile(data: { displayName?: string; username?: string }) {
  return request.post('/v1/auth/profile', data)
}

// 修改密码
export function changePassword(data: { currentPassword: string; newPassword: string }) {
  return request.post('/v1/auth/change-password', data)
}

// 查询登录日志
export function getLoginLogs(params: { page?: number; pageSize?: number }) {
  return request.get('/v1/auth/login-logs', { params })
}
