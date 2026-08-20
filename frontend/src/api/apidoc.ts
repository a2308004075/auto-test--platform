import request from './request'

/**
 * 接口文档模块 API
 */

// 接口列表
export function getApis(projectId: string, params?: any) {
  return request.get(`/v1/projects/${projectId}/apis`, { params })
}

export function getApi(projectId: string, apiId: string) {
  return request.get(`/v1/projects/${projectId}/apis/${apiId}`)
}

export function createApi(projectId: string, data: any) {
  return request.post(`/v1/projects/${projectId}/apis`, data)
}

export function updateApi(projectId: string, apiId: string, data: any) {
  return request.put(`/v1/projects/${projectId}/apis/${apiId}`, data)
}

export function deleteApi(projectId: string, apiId: string) {
  return request.delete(`/v1/projects/${projectId}/apis/${apiId}`)
}

export function batchDeleteApis(projectId: string, apiIds: string[]) {
  return request.post(`/v1/projects/${projectId}/apis/batch-delete`, apiIds)
}

export function batchMoveApis(projectId: string, targetModuleId: string, apiIds: string[]) {
  return request.post(`/v1/projects/${projectId}/apis/batch-move`, apiIds, { params: { targetModuleId } })
}

export function importSwagger(projectId: string, data: any) {
  return request.post(`/v1/projects/${projectId}/apis/swagger-import`, data)
}

export function debugApi(projectId: string, apiId: string, data: any) {
  return request.post(`/v1/projects/${projectId}/apis/${apiId}/debug`, data)
}

// 接口分组
export function getModules(projectId: string) {
  return request.get(`/v1/projects/${projectId}/modules`)
}

export function createModule(projectId: string, data: any) {
  return request.post(`/v1/projects/${projectId}/modules`, data)
}

export function updateModule(projectId: string, moduleId: string, data: any) {
  return request.put(`/v1/projects/${projectId}/modules/${moduleId}`, data)
}

export function deleteModule(projectId: string, moduleId: string) {
  return request.delete(`/v1/projects/${projectId}/modules/${moduleId}`)
}
