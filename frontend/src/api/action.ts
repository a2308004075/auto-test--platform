/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description Action 关键字模块 API
 */
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
  return request.post(`/v1/projects/${projectId}/actions/${actionId}`, data)
}

export function deleteAction(projectId: number, actionId: number) {
  return request.post(`/v1/projects/${projectId}/actions/${actionId}/delete`)
}

export function debugAction(projectId: number, actionId: number, data: any) {
  return request.post(`/v1/projects/${projectId}/actions/${actionId}/debug`, data)
}

export function getActionReferences(projectId: number, actionId: number) {
  return request.get(`/v1/projects/${projectId}/actions/${actionId}/references`)
}

// ===== Action 分组 API =====

export function getActionGroups(projectId: number) {
  return request.get(`/v1/projects/${projectId}/action-groups`)
}

export function createActionGroup(projectId: number, data: { parentId?: number | null; name: string; description?: string }) {
  return request.post(`/v1/projects/${projectId}/action-groups`, data)
}

export function updateActionGroup(projectId: number, groupId: number, data: { parentId?: number | null; name?: string; description?: string }) {
  return request.post(`/v1/projects/${projectId}/action-groups/${groupId}`, data)
}

export function deleteActionGroup(projectId: number, groupId: number) {
  return request.post(`/v1/projects/${projectId}/action-groups/${groupId}/delete`)
}

export function batchMoveActions(projectId: number, targetGroupId: number, actionIds: number[]) {
  return request.post(`/v1/projects/${projectId}/actions/batch-move`, { targetGroupId, actionIds })
}
