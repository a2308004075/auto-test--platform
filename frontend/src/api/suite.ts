/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 测试套件模块 API
 */
import request from './request'

/**
 * 测试套件模块 API（M8）
 */

export function getSuites(projectId: number, params?: { keyword?: string; groupId?: number | null; priority?: string; page?: number; pageSize?: number }) {
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
 * 批量查询套件通过率
 */
export function getSuitePassRates(projectId: number, suiteIds: number[]) {
  return request.post(`/v1/projects/${projectId}/suites/pass-rates`, suiteIds)
}

/**
 * 批量修改套件分组
 */
export function batchUpdateSuiteGroup(projectId: number, data: { suiteIds: number[]; groupId: number | null }) {
  return request.post(`/v1/projects/${projectId}/suites/batch-group`, data)
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

/* ===== 套件分组 API ===== */

export function getSuiteGroups(projectId: number) {
  return request.get(`/v1/projects/${projectId}/suite-groups`)
}

export function createSuiteGroup(projectId: number, data: { name: string; description?: string; parentId?: number | null; sortNo?: number }) {
  return request.post(`/v1/projects/${projectId}/suite-groups`, data)
}

export function updateSuiteGroup(projectId: number, groupId: number, data: { name: string; description?: string; parentId?: number | null; sortNo?: number }) {
  return request.post(`/v1/projects/${projectId}/suite-groups/${groupId}`, data)
}

export function deleteSuiteGroup(projectId: number, groupId: number) {
  return request.post(`/v1/projects/${projectId}/suite-groups/${groupId}/delete`)
}

/**
 * 清空分组及其子孙分组中的所有套件
 */
export function clearGroupSuites(projectId: number, groupId: number) {
  return request.post(`/v1/projects/${projectId}/suite-groups/${groupId}/clear-suites`)
}

/**
 * 清空项目下所有套件
 */
export function clearProjectSuites(projectId: number) {
  return request.post(`/v1/projects/${projectId}/suite-groups/clear-all-suites`)
}
