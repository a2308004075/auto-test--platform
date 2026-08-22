/**
 * @author HXN
 * @date 2026-08-22 13:28
 * @description 角色模块 API
 */
import request from './request'

/**
 * 角色管理模块 API（ADMIN）
 */

// 分页查询角色列表
export function getRolePage(params?: { keyword?: string; page?: number; pageSize?: number }) {
  return request.get('/v1/roles/page', { params })
}

// 查询角色详情（含权限 ID）
export function getRoleDetail(id: number) {
  return request.get(`/v1/roles/${id}`)
}

// 创建角色
export function createRole(data: {
  roleName: string
  roleCode: string
  description?: string
  sortOrder?: number
  permissionIds?: number[]
}) {
  return request.post('/v1/roles', data)
}

// 更新角色
export function updateRole(id: number, data: {
  roleName: string
  roleCode: string
  description?: string
  sortOrder?: number
  permissionIds?: number[]
}) {
  return request.post(`/v1/roles/${id}`, data)
}

// 删除角色
export function deleteRole(id: number) {
  return request.post(`/v1/roles/${id}/delete`)
}

// 切换角色状态
export function toggleRoleStatus(id: number, data: { isActive: number }) {
  return request.post(`/v1/roles/${id}/status`, data)
}

// 获取权限树
export function getPermissionTree() {
  return request.get('/v1/permissions/tree')
}

// 获取角色已分配的权限 ID 列表
export function getRolePermissionIds(id: number) {
  return request.get(`/v1/roles/${id}/permissions`)
}

// 分配权限
export function assignRolePermissions(id: number, permissionIds: number[]) {
  return request.post(`/v1/roles/${id}/permissions`, permissionIds)
}

// 导出角色 Excel
export function exportRoles() {
  return request.get('/v1/roles/export', { responseType: 'blob' })
}

// 导入角色 Excel
export function importRoles(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/v1/roles/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
