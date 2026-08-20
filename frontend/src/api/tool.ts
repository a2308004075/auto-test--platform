import request from './request'

/**
 * 工具方法模块 API
 */

export function getTools(projectId: string, params?: any) {
  return request.get(`/v1/projects/${projectId}/tools`, { params })
}

export function getTool(projectId: string, toolId: string) {
  return request.get(`/v1/projects/${projectId}/tools/${toolId}`)
}

export function createTool(projectId: string, data: any) {
  return request.post(`/v1/projects/${projectId}/tools`, data)
}

export function updateTool(projectId: string, toolId: string, data: any) {
  return request.put(`/v1/projects/${projectId}/tools/${toolId}`, data)
}

export function deleteTool(projectId: string, toolId: string) {
  return request.delete(`/v1/projects/${projectId}/tools/${toolId}`)
}

export function testTool(projectId: string, toolId: string, data: any) {
  return request.post(`/v1/projects/${projectId}/tools/${toolId}/test`, data)
}
