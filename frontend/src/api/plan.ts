import request from './request'

/**
 * 测试计划模块 API（M9）
 */

export function getPlans(projectId: string, params?: { keyword?: string; page?: number; pageSize?: number }) {
  return request.get(`/v1/projects/${projectId}/plans`, { params })
}

export function getPlan(planId: string) {
  return request.get(`/v1/plans/${planId}`)
}

export function createPlan(projectId: string, data: any) {
  return request.post(`/v1/projects/${projectId}/plans`, data)
}

export function updatePlan(planId: string, data: any) {
  return request.put(`/v1/plans/${planId}`, data)
}

export function deletePlan(planId: string) {
  return request.delete(`/v1/plans/${planId}`)
}
