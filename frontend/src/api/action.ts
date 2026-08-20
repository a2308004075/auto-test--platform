import request from './request'

/**
 * Action 关键字模块 API
 */

export function getActions(projectId: number, params?: any) {
  return request.get(`/v1/projects/${projectId}/actions`, { params })
}

export function getAction(projectId: number, actionId: number) {
  return request.get(`/v1/projects/${projectId}/actions/${actionId}`)
}

export function createAction(projectId: number, data: any) {
  return request.post(`/v1/projects/${projectId}/actions`, data)
}

export function updateAction(projectId: number, actionId: number, data: any) {
  return request.put(`/v1/projects/${projectId}/actions/${actionId}`, data)
}

export function deleteAction(projectId: number, actionId: number) {
  return request.delete(`/v1/projects/${projectId}/actions/${actionId}`)
}

export function debugAction(projectId: number, actionId: number, data: any) {
  return request.post(`/v1/projects/${projectId}/actions/${actionId}/debug`, data)
}

export function getActionReferences(projectId: number, actionId: number) {
  return request.get(`/v1/projects/${projectId}/actions/${actionId}/references`)
}
