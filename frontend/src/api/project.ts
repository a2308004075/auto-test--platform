import request from './request'

/**
 * 项目管理模块 API
 */

// 获取项目列表
export function getProjects() {
  return request.get('/v1/projects')
}

// 获取项目详情
export function getProject(id: number) {
  return request.get(`/v1/projects/${id}`)
}

// 创建项目
export function createProject(data: { name: string; description?: string }) {
  return request.post('/v1/projects', data)
}

// 更新项目
export function updateProject(id: number, data: { name?: string; description?: string }) {
  return request.put(`/v1/projects/${id}`, data)
}

// 删除项目
export function deleteProject(id: number) {
  return request.delete(`/v1/projects/${id}`)
}
