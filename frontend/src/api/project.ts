import request from './request'

/**
 * 项目管理模块 API
 */

export function getProjects(params?: { keyword?: string; status?: boolean; page?: number; pageSize?: number }) {
  return request.get('/v1/projects', { params })
}

export function getProject(id: number) {
  return request.get(`/v1/projects/${id}`)
}

export function createProject(data: { name: string; description?: string }) {
  return request.post('/v1/projects', data)
}

export function updateProject(id: number, data: { name?: string; description?: string }) {
  return request.put(`/v1/projects/${id}`, data)
}

export function deleteProject(id: number) {
  return request.delete(`/v1/projects/${id}`)
}

export function toggleProjectStatus(id: number) {
  return request.patch(`/v1/projects/${id}/status`)
}

export function getProjectDashboard(id: number) {
  return request.get(`/v1/projects/${id}/dashboard`)
}
