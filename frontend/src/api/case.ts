/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 测试用例模块 API
 */
import request from './request'

/**
 * 测试用例模块 API（M8）
 */

export function getCases(projectId: number, params?: { suiteId?: number; keyword?: string; page?: number; pageSize?: number }) {
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
