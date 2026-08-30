/**
 * @author HXN
 * @date 2026-08-30
 * @description 手动用例模块 API
 */
import request from './request'

// ===== 手动用例 API =====

export function getManualCases(projectId: number, params?: {
  groupId?: number; keyword?: string; priority?: string;
  caseType?: string; caseStatus?: string; page?: number; pageSize?: number
}) {
  return request.get(`/v1/projects/${projectId}/manual-cases`, { params })
}

export function getManualCase(projectId: number, caseId: number) {
  return request.get(`/v1/projects/${projectId}/manual-cases/${caseId}`)
}

export function createManualCase(projectId: number, data: any) {
  return request.post(`/v1/projects/${projectId}/manual-cases`, data)
}

export function updateManualCase(projectId: number, caseId: number, data: any) {
  return request.post(`/v1/projects/${projectId}/manual-cases/${caseId}`, data)
}

export function deleteManualCase(projectId: number, caseId: number) {
  return request.post(`/v1/projects/${projectId}/manual-cases/${caseId}/delete`)
}

export function toggleManualCaseStatus(projectId: number, caseId: number) {
  return request.post(`/v1/projects/${projectId}/manual-cases/${caseId}/status`)
}

// ===== 手动用例分组 API =====

export function getManualCaseGroups(projectId: number) {
  return request.get(`/v1/projects/${projectId}/manual-case-groups`)
}

export function createManualCaseGroup(projectId: number, data: { parentId?: number | null; name: string; description?: string }) {
  return request.post(`/v1/projects/${projectId}/manual-case-groups`, data)
}

export function updateManualCaseGroup(projectId: number, groupId: number, data: { parentId?: number | null; name?: string; description?: string }) {
  return request.post(`/v1/projects/${projectId}/manual-case-groups/${groupId}`, data)
}

export function deleteManualCaseGroup(projectId: number, groupId: number) {
  return request.post(`/v1/projects/${projectId}/manual-case-groups/${groupId}/delete`)
}

export function clearManualGroupCases(projectId: number, groupId: number) {
  return request.post(`/v1/projects/${projectId}/manual-case-groups/${groupId}/clear-cases`)
}

export function clearManualProjectCases(projectId: number) {
  return request.post(`/v1/projects/${projectId}/manual-case-groups/clear-all-cases`)
}
