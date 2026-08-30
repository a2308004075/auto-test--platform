/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 测试用例模块 API
 */
import request from './request'

/**
 * 测试用例模块 API（M8）
 */

export function getCases(projectId: number, params?: {
  suiteId?: number; groupId?: number; keyword?: string;
  priority?: string; status?: string; page?: number; pageSize?: number
}) {
  return request.get(`/v1/projects/${projectId}/cases`, { params })
}

export function getCase(projectId: number, caseId: number) {
  return request.get(`/v1/projects/${projectId}/cases/${caseId}`)
}

export function createCase(projectId: number, data: any) {
  return request.post(`/v1/projects/${projectId}/cases`, data)
}

export function updateCase(projectId: number, caseId: number, data: any) {
  return request.post(`/v1/projects/${projectId}/cases/${caseId}`, data)
}

export function deleteCase(projectId: number, caseId: number) {
  return request.post(`/v1/projects/${projectId}/cases/${caseId}/delete`)
}

export function toggleCaseStatus(projectId: number, caseId: number) {
  return request.post(`/v1/projects/${projectId}/cases/${caseId}/status`)
}

export function debugCase(projectId: number, caseId: number, data?: { environmentId?: number }) {
  return request.post(`/v1/projects/${projectId}/cases/${caseId}/debug`, data || {})
}

// ===== 用例分组 API =====

export function getCaseGroups(projectId: number) {
  return request.get(`/v1/projects/${projectId}/case-groups`)
}

export function createCaseGroup(projectId: number, data: { parentId?: number | null; name: string; description?: string }) {
  return request.post(`/v1/projects/${projectId}/case-groups`, data)
}

export function updateCaseGroup(projectId: number, groupId: number, data: { parentId?: number | null; name?: string; description?: string }) {
  return request.post(`/v1/projects/${projectId}/case-groups/${groupId}`, data)
}

export function deleteCaseGroup(projectId: number, groupId: number) {
  return request.post(`/v1/projects/${projectId}/case-groups/${groupId}/delete`)
}

/**
 * 清空分组及其子孙分组中的所有用例
 */
export function clearGroupCases(projectId: number, groupId: number) {
  return request.post(`/v1/projects/${projectId}/case-groups/${groupId}/clear-cases`)
}

/**
 * 清空项目下所有用例
 */
export function clearProjectCases(projectId: number) {
  return request.post(`/v1/projects/${projectId}/case-groups/clear-all-cases`)
}
