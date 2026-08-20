import request from './request'

/**
 * 测试执行模块 API（M9）
 */

export function startExecution(planId: string, data?: { environmentId?: string; triggerType?: string }) {
  return request.post(`/v1/plans/${planId}/executions`, data || {})
}

export function getExecutions(projectId: string, params?: { status?: string; page?: number; pageSize?: number }) {
  return request.get(`/v1/projects/${projectId}/executions`, { params })
}

export function getExecution(executionId: string) {
  return request.get(`/v1/executions/${executionId}`)
}

export function getExecutionResults(executionId: string) {
  return request.get(`/v1/executions/${executionId}/results`)
}

export function cancelExecution(executionId: string) {
  return request.post(`/v1/executions/${executionId}/cancel`)
}
