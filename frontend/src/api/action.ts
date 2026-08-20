import request from './request'

/**
 * Action 关键字模块 API
 */

export function getActions(projectId: string, params?: any) {
  return request.get(`/v1/projects/${projectId}/actions`, { params })
}

export function getAction(projectId: string, actionId: string) {
  return request.get(`/v1/projects/${projectId}/actions/${actionId}`)
}

export function createAction(projectId: string, data: any) {
  return request.post(`/v1/projects/${projectId}/actions`, data)
}

export function updateAction(projectId: string, actionId: string, data: any) {
  return request.put(`/v1/projects/${projectId}/actions/${actionId}`, data)
}

export function deleteAction(projectId: string, actionId: string) {
  return request.delete(`/v1/projects/${projectId}/actions/${actionId}`)
}

export function debugAction(projectId: string, actionId: string, data: any) {
  return request.post(`/v1/projects/${projectId}/actions/${actionId}/debug`, data)
}

export function getActionReferences(projectId: string, actionId: string) {
  return request.get(`/v1/projects/${projectId}/actions/${actionId}/references`)
}
