/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 自动化用例模块 API
 */
import request from './request'

/**
 * 自动化用例模块 API（M8）
 */

export function getAutoCases(projectId: number, params?: {
  autoSuiteId?: number; groupId?: number; keyword?: string;
  priority?: string; status?: string; page?: number; pageSize?: number
}) {
  return request.get(`/v1/projects/${projectId}/auto-cases`, { params })
}

export function getAutoCase(projectId: number, autoCaseId: number) {
  return request.get(`/v1/projects/${projectId}/auto-cases/${autoCaseId}`)
}

export function createAutoCase(projectId: number, data: any) {
  return request.post(`/v1/projects/${projectId}/auto-cases`, data)
}

export function updateAutoCase(projectId: number, autoCaseId: number, data: any) {
  return request.post(`/v1/projects/${projectId}/auto-cases/${autoCaseId}`, data)
}

export function deleteAutoCase(projectId: number, autoCaseId: number) {
  return request.post(`/v1/projects/${projectId}/auto-cases/${autoCaseId}/delete`)
}

export function toggleAutoCaseStatus(projectId: number, autoCaseId: number) {
  return request.post(`/v1/projects/${projectId}/auto-cases/${autoCaseId}/status`)
}

export function debugAutoCase(projectId: number, autoCaseId: number, data?: { environmentId?: number }) {
  return request.post(`/v1/projects/${projectId}/auto-cases/${autoCaseId}/debug`, data || {})
}

// ===== 自动化用例分组 API =====

export function getAutoCaseGroups(projectId: number) {
  return request.get(`/v1/projects/${projectId}/auto-case-groups`)
}

export function createAutoCaseGroup(projectId: number, data: { parentId?: number | null; name: string; description?: string }) {
  return request.post(`/v1/projects/${projectId}/auto-case-groups`, data)
}

export function updateAutoCaseGroup(projectId: number, groupId: number, data: { parentId?: number | null; name?: string; description?: string }) {
  return request.post(`/v1/projects/${projectId}/auto-case-groups/${groupId}`, data)
}

export function deleteAutoCaseGroup(projectId: number, groupId: number) {
  return request.post(`/v1/projects/${projectId}/auto-case-groups/${groupId}/delete`)
}

/**
 * 清空分组及其子孙分组中的所有自动化用例
 */
export function clearGroupAutoCases(projectId: number, groupId: number) {
  return request.post(`/v1/projects/${projectId}/auto-case-groups/${groupId}/clear-cases`)
}

/**
 * 清空项目下所有自动化用例
 */
export function clearProjectAutoCases(projectId: number) {
  return request.post(`/v1/projects/${projectId}/auto-case-groups/clear-all-cases`)
}
