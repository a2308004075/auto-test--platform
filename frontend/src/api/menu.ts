/**
 * @author HXN
 * @date 2026-08-22 13:28
 * @description 菜单模块 API
 */
import request from './request'

/**
 * 菜单管理模块 API
 */

export interface MenuTreeNode {
  id: number
  parentId: number
  name: string
  menuType: number
  icon: string
  routePath: string
  component: string
  sortNo: number
  isActive: number
  permissionCode: string | null
  children?: MenuTreeNode[]
}

export interface MenuListItem {
  id: number
  parentId: number
  name: string
  menuType: number
  icon: string
  routePath: string
  component: string
  sortNo: number
  isActive: number
  permissionCode: string | null
  createdAt: string
  updatedAt: string
}

export interface MenuCreateRequest {
  parentId?: number
  name: string
  menuType: number
  icon?: string
  routePath?: string
  component?: string
  sortNo?: number
  permissionCode?: string
}

/** 获取菜单树（仅启用状态） */
export function getMenuTree() {
  return request.get('/v1/sys/menus/tree')
}

/** 获取所有菜单列表（扁平） */
export function getMenuList() {
  return request.get('/v1/sys/menus')
}

/** 获取单个菜单 */
export function getMenu(id: number) {
  return request.get(`/v1/sys/menus/${id}`)
}

/** 新增菜单 */
export function addMenu(data: MenuCreateRequest) {
  return request.post('/v1/sys/menus', data)
}

/** 更新菜单 */
export function updateMenu(id: number, data: MenuCreateRequest) {
  return request.post(`/v1/sys/menus/${id}`, data)
}

/** 删除菜单 */
export function deleteMenu(id: number) {
  return request.delete(`/v1/sys/menus/${id}`)
}

/** 切换菜单启用/停用 */
export function toggleMenuStatus(id: number) {
  return request.post(`/v1/sys/menus/${id}/toggle`)
}

/** 菜单 Excel 导入结果 */
export interface MenuImportResult {
  successCount: number
  failCount: number
  errors: string[]
}

/** 导出菜单 Excel */
export function exportMenus() {
  return request.get('/v1/sys/menus/export', { responseType: 'blob' })
}

/** 导入菜单 Excel */
export function importMenus(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/v1/sys/menus/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
