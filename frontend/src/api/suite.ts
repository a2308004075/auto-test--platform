import request from './request'

/**
 * 测试套件模块 API（M8）
 */

export function getSuites(projectId: string, params?: { keyword?: string; page?: number; pageSize?: number }) {
  return request.get(`/v1/projects/${projectId}/suites`, { params })
}

export function getSuite(projectId: string, suiteId: string) {
  return request.get(`/v1/projects/${projectId}/suites/${suiteId}`)
}

export function createSuite(projectId: string, data: any) {
  return request.post(`/v1/projects/${projectId}/suites`, data)
}

export function updateSuite(projectId: string, suiteId: string, data: any) {
  return request.put(`/v1/projects/${projectId}/suites/${suiteId}`, data)
}

export function deleteSuite(projectId: string, suiteId: string) {
  return request.delete(`/v1/projects/${projectId}/suites/${suiteId}`)
}
