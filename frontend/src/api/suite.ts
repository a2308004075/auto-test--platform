/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 测试套件模块 API
 */
import request from './request'

/**
 * 测试套件模块 API（M8）
 */

export function getSuites(projectId: number, params?: { keyword?: string; page?: number; pageSize?: number }) {
  return request.get(`/v1/projects/${projectId}/suites`, { params })
}

export function getSuite(projectId: number, suiteId: number) {
  return request.get(`/v1/projects/${projectId}/suites/${suiteId}`)
}

export function createSuite(projectId: number, data: any) {
  return request.post(`/v1/projects/${projectId}/suites`, data)
}

export function updateSuite(projectId: number, suiteId: number, data: any) {
  return request.post(`/v1/projects/${projectId}/suites/${suiteId}`, data)
}

export function deleteSuite(projectId: number, suiteId: number) {
  return request.post(`/v1/projects/${projectId}/suites/${suiteId}/delete`)
}

/**
 * 获取套件内用例级生命周期配置
 */
export function getSuiteLifecycle(projectId: number, suiteId: number) {
  return request.get(`/v1/projects/${projectId}/suites/${suiteId}/lifecycle`)
}

/**
 * 保存套件内用例级生命周期配置
 */
export function saveSuiteLifecycle(projectId: number, suiteId: number, data: any) {
  return request.put(`/v1/projects/${projectId}/suites/${suiteId}/lifecycle`, data)
}
