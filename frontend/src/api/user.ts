import request from './request'

/**
 * 用户管理模块 API（ADMIN）
 */

export function getUsers(params?: { keyword?: string; roleId?: number; page?: number; pageSize?: number }) {
  return request.get('/v1/users', { params })
}

export function createUser(data: any) {
  return request.post('/v1/users', data)
}

export function updateUser(userId: number, data: any) {
  return request.put(`/v1/users/${userId}`, data)
}

export function deleteUser(userId: number) {
  return request.delete(`/v1/users/${userId}`)
}

export function toggleUserStatus(userId: number, data: { isActive: number }) {
  return request.patch(`/v1/users/${userId}/status`, data)
}

export function resetPassword(userId: number, data: { newPassword: string }) {
  return request.post(`/v1/users/${userId}/reset-password`, data)
}

/**
 * 获取角色列表
 */
export function getRoles() {
  return request.get('/v1/roles')
}
