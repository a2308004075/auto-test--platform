import request from './request'

/**
 * 测试用例模块 API（M8）
 */

export function getCases(projectId: string, params?: { suiteId?: string; keyword?: string; page?: number; pageSize?: number }) {
  return request.get(`/v1/projects/${projectId}/cases`, { params })
}

export function getCase(projectId: string, caseId: string) {
  return request.get(`/v1/projects/${projectId}/cases/${caseId}`)
}

export function createCase(projectId: string, data: any) {
  return request.post(`/v1/projects/${projectId}/cases`, data)
}

export function updateCase(projectId: string, caseId: string, data: any) {
  return request.put(`/v1/projects/${projectId}/cases/${caseId}`, data)
}

export function deleteCase(projectId: string, caseId: string) {
  return request.delete(`/v1/projects/${projectId}/cases/${caseId}`)
}

export function toggleCaseStatus(projectId: string, caseId: string) {
  return request.patch(`/v1/projects/${projectId}/cases/${caseId}/status`)
}
