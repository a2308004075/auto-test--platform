/**
 * @author HXN
 * @date 2026-08-18 17:31
 * @description 项目模块 API
 */
import request from './request'

/**
 * 项目管理模块 API
 */

export function getProjects(params?: { keyword?: string; status?: number; page?: number; pageSize?: number }) {
  return request.get('/v1/projects', { params })
}

export function getProject(id: number) {
  return request.get(`/v1/projects/${id}`)
}

export function createProject(data: { name: string; description?: string }) {
  return request.post('/v1/projects', data)
}

export function updateProject(id: number, data: { name?: string; description?: string }) {
  return request.post(`/v1/projects/${id}`, data)
}

export function deleteProject(id: number) {
  return request.post(`/v1/projects/${id}/delete`)
}

export function toggleProjectStatus(id: number) {
  return request.post(`/v1/projects/${id}/status`)
}

export function getProjectDashboard(id: number) {
  return request.get(`/v1/projects/${id}/dashboard`)
}
