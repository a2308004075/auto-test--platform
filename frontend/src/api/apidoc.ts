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

export function importSwagger(projectId: number, data: any) {
  return request.post(`/v1/projects/${projectId}/apis/swagger-import`, data)
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

// 接口被关键字引用的关系（后端端点待实现，调用需 try/catch 容错）
export function getApiReferences(projectId: number, apiId: number) {
  return request.get(`/v1/projects/${projectId}/apis/${apiId}/references`)
}
