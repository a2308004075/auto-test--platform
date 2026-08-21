import request from './request'

/**
 * 工具方法模块 API
 */

export function getTools(projectId: number, params?: any) {
  return request.get(`/v1/projects/${projectId}/tools`, { params })
}

export function getTool(projectId: number, toolId: number) {
  return request.get(`/v1/projects/${projectId}/tools/${toolId}`)
}

export function createTool(projectId: number, data: any) {
  return request.post(`/v1/projects/${projectId}/tools`, data)
}

export function updateTool(projectId: number, toolId: number, data: any) {
  return request.post(`/v1/projects/${projectId}/tools/${toolId}`, data)
}

export function deleteTool(projectId: number, toolId: number) {
  return request.post(`/v1/projects/${projectId}/tools/${toolId}/delete`)
}

export function testTool(projectId: number, toolId: number, data: any) {
  return request.post(`/v1/projects/${projectId}/tools/${toolId}/test`, data)
}
