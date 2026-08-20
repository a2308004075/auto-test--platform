import request from './request'

/**
 * 环境配置模块 API
 */

export function getEnvironments(projectId: string) {
  return request.get(`/v1/projects/${projectId}/environments`)
}

export function getEnvironment(projectId: string, envId: string) {
  return request.get(`/v1/projects/${projectId}/environments/${envId}`)
}

export function createEnvironment(projectId: string, data: any) {
  return request.post(`/v1/projects/${projectId}/environments`, data)
}

export function updateEnvironment(projectId: string, envId: string, data: any) {
  return request.put(`/v1/projects/${projectId}/environments/${envId}`, data)
}

export function deleteEnvironment(projectId: string, envId: string) {
  return request.delete(`/v1/projects/${projectId}/environments/${envId}`)
}

export function activateEnvironment(projectId: string, envId: string) {
  return request.patch(`/v1/projects/${projectId}/environments/${envId}/activate`)
}

export function testEnvironment(projectId: string, envId: string) {
  return request.post(`/v1/projects/${projectId}/environments/${envId}/test`)
}
