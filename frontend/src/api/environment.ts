/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 环境配置模块 API
 */
import request from './request'

/**
 * 环境配置模块 API
 */

export function getEnvironments(projectId: number) {
  return request.get(`/v1/projects/${projectId}/environments`)
}

export function getEnvironment(projectId: number, envId: number) {
  return request.get(`/v1/projects/${projectId}/environments/${envId}`)
}

export function createEnvironment(projectId: number, data: any) {
  return request.post(`/v1/projects/${projectId}/environments`, data)
}

export function updateEnvironment(projectId: number, envId: number, data: any) {
  return request.post(`/v1/projects/${projectId}/environments/${envId}`, data)
}

export function deleteEnvironment(projectId: number, envId: number) {
  return request.post(`/v1/projects/${projectId}/environments/${envId}/delete`)
}

/**
 * 项目全局变量（不绑定环境，整个项目任何地方可引用）
 */
export function getProjectVariables(projectId: number) {
  return request.get(`/v1/projects/${projectId}/variables`)
}

export function updateProjectVariables(projectId: number, data: any) {
  return request.post(`/v1/projects/${projectId}/variables`, data)
}
