/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 自动化套件模块 API
 */
import request from './request'

/**
 * 自动化套件模块 API（M8）
 */

export function getAutoSuites(projectId: number, params?: { keyword?: string; groupId?: number | null; priority?: string; page?: number; pageSize?: number }) {
  return request.get(`/v1/projects/${projectId}/auto-suites`, { params })
}

export function getAutoSuite(projectId: number, autoSuiteId: number) {
  return request.get(`/v1/projects/${projectId}/auto-suites/${autoSuiteId}`)
}

export function createAutoSuite(projectId: number, data: any) {
  return request.post(`/v1/projects/${projectId}/auto-suites`, data)
}

export function updateAutoSuite(projectId: number, autoSuiteId: number, data: any) {
  return request.post(`/v1/projects/${projectId}/auto-suites/${autoSuiteId}`, data)
}

export function deleteAutoSuite(projectId: number, autoSuiteId: number) {
  return request.post(`/v1/projects/${projectId}/auto-suites/${autoSuiteId}/delete`)
}

/**
 * 批量查询自动化套件通过率
 */
export function getAutoSuitePassRates(projectId: number, autoSuiteIds: number[]) {
  return request.post(`/v1/projects/${projectId}/auto-suites/pass-rates`, autoSuiteIds)
}

/**
 * 批量修改自动化套件分组
 */
export function batchUpdateAutoSuiteGroup(projectId: number, data: { autoSuiteIds: number[]; groupId: number | null }) {
  return request.post(`/v1/projects/${projectId}/auto-suites/batch-group`, data)
}

/**
 * 获取自动化套件内自动化用例级生命周期配置
 */
export function getAutoSuiteLifecycle(projectId: number, autoSuiteId: number) {
  return request.get(`/v1/projects/${projectId}/auto-suites/${autoSuiteId}/lifecycle`)
}

/**
 * 保存自动化套件内自动化用例级生命周期配置
 */
export function saveAutoSuiteLifecycle(projectId: number, autoSuiteId: number, data: any) {
  return request.put(`/v1/projects/${projectId}/auto-suites/${autoSuiteId}/lifecycle`, data)
}

/* ===== 自动化套件分组 API ===== */

export function getAutoSuiteGroups(projectId: number) {
  return request.get(`/v1/projects/${projectId}/auto-suite-groups`)
}

export function createAutoSuiteGroup(projectId: number, data: { name: string; description?: string; parentId?: number | null; sortNo?: number }) {
  return request.post(`/v1/projects/${projectId}/auto-suite-groups`, data)
}

export function updateAutoSuiteGroup(projectId: number, groupId: number, data: { name: string; description?: string; parentId?: number | null; sortNo?: number }) {
  return request.post(`/v1/projects/${projectId}/auto-suite-groups/${groupId}`, data)
}

export function deleteAutoSuiteGroup(projectId: number, groupId: number) {
  return request.post(`/v1/projects/${projectId}/auto-suite-groups/${groupId}/delete`)
}

/**
 * 清空分组及其子孙分组中的所有自动化套件
 */
export function clearGroupAutoSuites(projectId: number, groupId: number) {
  return request.post(`/v1/projects/${projectId}/auto-suite-groups/${groupId}/clear-suites`)
}

/**
 * 清空项目下所有自动化套件
 */
export function clearProjectAutoSuites(projectId: number) {
  return request.post(`/v1/projects/${projectId}/auto-suite-groups/clear-all-suites`)
}
