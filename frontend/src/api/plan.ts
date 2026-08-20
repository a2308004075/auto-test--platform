import request from './request'

/**
 * 测试计划模块 API（M9）
 */

export function getPlans(projectId: number, params?: { keyword?: string; page?: number; pageSize?: number }) {
  return request.get(`/v1/projects/${projectId}/plans`, { params })
}

export function getPlan(planId: number) {
  return request.get(`/v1/plans/${planId}`)
}

export function createPlan(projectId: number, data: any) {
  return request.post(`/v1/projects/${projectId}/plans`, data)
}

export function updatePlan(planId: number, data: any) {
  return request.put(`/v1/plans/${planId}`, data)
}

export function deletePlan(planId: number) {
  return request.delete(`/v1/plans/${planId}`)
}
