/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 执行模块 API
 */
import request from './request'

/**
 * 测试执行模块 API（M9）
 */

export function startExecution(planId: number, data?: { environmentId?: number; triggerType?: string }) {
  return request.post(`/v1/plans/${planId}/executions`, data || {})
}

export function getExecutions(projectId: number, params?: { status?: string; page?: number; pageSize?: number }) {
  return request.get(`/v1/projects/${projectId}/executions`, { params })
}

export function getExecution(executionId: number) {
  return request.get(`/v1/executions/${executionId}`)
}

export function getExecutionResults(executionId: number) {
  return request.get(`/v1/executions/${executionId}/results`)
}

export function cancelExecution(executionId: number) {
  return request.post(`/v1/executions/${executionId}/cancel`)
}
