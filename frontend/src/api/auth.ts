import request from './request'

/**
 * 认证模块 API
 */

// 登录
export function login(data: { username: string; password: string }) {
  return request.post('/v1/auth/login', data)
}

// 获取当前用户信息
export function getCurrentUser() {
  return request.get('/v1/auth/current-user')
}

// 退出登录
export function logout() {
  return request.post('/v1/auth/logout')
}
