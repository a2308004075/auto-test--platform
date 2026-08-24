/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description API 接口模块 API
 */
import request from './request'

/**
 * 接口文档模块 API
 */

// 接口列表
export function getApis(projectId: number, params?: any) {
  return request.get(`/v1/projects/${projectId}/apis`, { params })
}

export function getApi(projectId: number, apiId: number) {
  return request.get(`/v1/projects/${projectId}/apis/${apiId}`)
}

export function createApi(projectId: number, data: any) {
  return request.post(`/v1/projects/${projectId}/apis`, data)
}

export function updateApi(projectId: number, apiId: number, data: any) {
  return request.post(`/v1/projects/${projectId}/apis/${apiId}`, data)
}

export function deleteApi(projectId: number, apiId: number) {
  return request.post(`/v1/projects/${projectId}/apis/${apiId}/delete`)
}

export function batchDeleteApis(projectId: number, apiIds: number[]) {
  return request.post(`/v1/projects/${projectId}/apis/batch-delete`, apiIds)
}

export function batchMoveApis(projectId: number, targetModuleId: number, apiIds: number[]) {
  return request.post(`/v1/projects/${projectId}/apis/batch-move`, apiIds, { params: { targetModuleId } })
}

export function syncSwaggerUrl(projectId: number, data: { url: string; moduleId: number; headers?: Record<string, string> }) {
  return request.post(`/v1/projects/${projectId}/apis/swagger-sync-url`, data)
}

// Swagger 同步配置
export function getSyncConfigs(projectId: number) {
  return request.get(`/v1/projects/${projectId}/apis/sync-configs`)
}

export function createSyncConfig(projectId: number, data: { name: string; url: string; moduleId: number; headers?: string; authUsername?: string; authPassword?: string }) {
  return request.post(`/v1/projects/${projectId}/apis/sync-configs`, data)
}

export function updateSyncConfig(projectId: number, configId: number, data: { name: string; url: string; moduleId: number; headers?: string; authUsername?: string; authPassword?: string }) {
  return request.post(`/v1/projects/${projectId}/apis/sync-configs/${configId}`, data)
}

export function deleteSyncConfig(projectId: number, configId: number) {
  return request.post(`/v1/projects/${projectId}/apis/sync-configs/${configId}/delete`)
}

export function syncOneConfig(projectId: number, configId: number) {
  return request.post(`/v1/projects/${projectId}/apis/sync-configs/${configId}/sync`)
}

export function syncAllConfigs(projectId: number) {
  return request.post(`/v1/projects/${projectId}/apis/sync-configs/sync-all`)
}

export function debugApi(projectId: number, apiId: number, data: any) {
  return request.post(`/v1/projects/${projectId}/apis/${apiId}/debug`, data)
}

// 接口分组
export function getModules(projectId: number) {
  return request.get(`/v1/projects/${projectId}/modules`)
}

export function createModule(projectId: number, data: any) {
  return request.post(`/v1/projects/${projectId}/modules`, data)
}

export function updateModule(projectId: number, moduleId: number, data: any) {
  return request.post(`/v1/projects/${projectId}/modules/${moduleId}`, data)
}

export function deleteModule(projectId: number, moduleId: number) {
  return request.post(`/v1/projects/${projectId}/modules/${moduleId}/delete`)
}

// 接口被关键字引用的关系
export function getApiReferences(projectId: number, apiId: number) {
  return request.get(`/v1/projects/${projectId}/apis/${apiId}/references`)
}
