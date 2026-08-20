import request from './request'

/**
 * 接口关键字模块 API
 */

export function getKeywords(projectId: string, params?: any) {
  return request.get(`/v1/projects/${projectId}/keywords`, { params })
}

export function getKeyword(projectId: string, keywordId: string) {
  return request.get(`/v1/projects/${projectId}/keywords/${keywordId}`)
}

export function createKeyword(projectId: string, data: any) {
  return request.post(`/v1/projects/${projectId}/keywords`, data)
}

export function updateKeyword(projectId: string, keywordId: string, data: any) {
  return request.put(`/v1/projects/${projectId}/keywords/${keywordId}`, data)
}

export function deleteKeyword(projectId: string, keywordId: string) {
  return request.delete(`/v1/projects/${projectId}/keywords/${keywordId}`)
}

export function generateKeyword(projectId: string, apiId: string) {
  return request.post(`/v1/projects/${projectId}/keywords/generate`, null, { params: { apiId } })
}
