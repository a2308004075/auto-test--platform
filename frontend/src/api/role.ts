/**
 * @author HXN
 * @date 2026-08-22 13:28
 * @description 角色模块 API
 */
import request from './request'

/**
 * 角色管理模块 API（ADMIN）
 */

/**
 * 权限分配项（含按角色 control_mode）
 */
export interface PermissionAssignment {
  permissionId: number
  controlMode: string | null
}

// 分页查询角色列表
export function getRolePage(params?: { keyword?: string; page?: number; pageSize?: number }) {
  return request.get('/v1/roles/page', { params })
}

// 查询角色详情（含权限分配列表）
export function getRoleDetail(id: number) {
  return request.get(`/v1/roles/${id}`)
}

// 创建角色
export function createRole(data: {
  roleName: string
  roleCode: string
  description?: string
  sortOrder?: number
  permissions?: PermissionAssignment[]
}) {
  return request.post('/v1/roles', data)
}

// 更新角色
export function updateRole(id: number, data: {
  roleName: string
  roleCode: string
  description?: string
  sortOrder?: number
  permissions?: PermissionAssignment[]
}) {
  return request.post(`/v1/roles/${id}`, data)
}

// 删除角色
export function deleteRole(id: number) {
  return request.post(`/v1/roles/${id}/delete`)
}

// 获取权限树
export function getPermissionTree() {
  return request.get('/v1/permissions/tree')
}

// 同步权限（从 sys_menu 同步页面和按钮到 permission 表）
export function syncPermissions() {
  return request.post('/v1/permissions/sync')
}

// 获取角色已分配的权限列表（含按角色 control_mode）
export function getRolePermissions(id: number) {
  return request.get(`/v1/roles/${id}/permissions`)
}

// 分配权限（含按角色 control_mode）
export function assignRolePermissions(id: number, permissions: PermissionAssignment[]) {
  return request.post(`/v1/roles/${id}/permissions`, permissions)
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
