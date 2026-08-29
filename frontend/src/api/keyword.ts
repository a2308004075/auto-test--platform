/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 关键字模块 API
 */
import request from './request'

/**
 * 接口关键字模块 API
 */

export function getKeywords(projectId: number, params?: any) {
  return request.get(`/v1/projects/${projectId}/keywords`, { params })
}

export function getKeyword(projectId: number, keywordId: number) {
  return request.get(`/v1/projects/${projectId}/keywords/${keywordId}`)
}

export function createKeyword(projectId: number, data: any) {
  return request.post(`/v1/projects/${projectId}/keywords`, data)
}

export function updateKeyword(projectId: number, keywordId: number, data: any) {
  return request.post(`/v1/projects/${projectId}/keywords/${keywordId}`, data)
}

export function deleteKeyword(projectId: number, keywordId: number) {
  return request.post(`/v1/projects/${projectId}/keywords/${keywordId}/delete`)
}

export function debugKeyword(projectId: number, keywordId: number, data: { environmentId: number }) {
  return request.post(`/v1/projects/${projectId}/keywords/${keywordId}/debug`, data)
}

export function getKeywordDependencies(projectId: number, keywordId: number) {
  return request.get(`/v1/projects/${projectId}/keywords/${keywordId}/dependencies`)
}

// 接口关键字分组
export function getKeywordGroups(projectId: number) {
  return request.get(`/v1/projects/${projectId}/keyword-groups`)
}

export function createKeywordGroup(projectId: number, data: any) {
  return request.post(`/v1/projects/${projectId}/keyword-groups`, data)
}

export function updateKeywordGroup(projectId: number, groupId: number, data: any) {
  return request.post(`/v1/projects/${projectId}/keyword-groups/${groupId}`, data)
}

export function deleteKeywordGroup(projectId: number, groupId: number) {
  return request.post(`/v1/projects/${projectId}/keyword-groups/${groupId}/delete`)
}

export function clearKeywordGroupKeywords(projectId: number, groupId: number) {
  return request.post(`/v1/projects/${projectId}/keyword-groups/${groupId}/clear-keywords`)
}

export function clearAllKeywords(projectId: number) {
  return request.post(`/v1/projects/${projectId}/keyword-groups/clear-all-keywords`)
}
