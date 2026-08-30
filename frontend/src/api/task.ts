/**
 * @author HXN
 * @date 2026-08-30
 * @description 通用任务模块 API
 */
import request from './request'

// ===== 任务类型常量 =====

export const TASK_TYPE_MAP: Record<string, string> = {
  REQUIREMENT_REVIEW: '需求评审',
  CASE_REVIEW: '用例评审',
  REQUIREMENT_MODIFY: '需求修改',
  CASE_MODIFY: '用例修改',
  CASE_EXECUTION: '用例执行',
  DEFECT_HANDLING: '缺陷处理',
}

export const TASK_STATUS_MAP: Record<string, string> = {
  PENDING: '待处理',
  IN_PROGRESS: '进行中',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
}

export const TASK_STATUS_TYPE_MAP: Record<string, string> = {
  PENDING: 'warning',
  IN_PROGRESS: '',
  COMPLETED: 'success',
  CANCELLED: 'info',
}

export const TASK_PRIORITY_TYPE_MAP: Record<string, string> = {
  '高': 'danger',
  '中': 'warning',
  '低': 'info',
}

// ===== API =====

export function getMyTasks(params?: { userId?: number; taskType?: string; status?: string }) {
  return request.get('/v1/tasks/my-tasks', { params })
}

export function getMyTaskCount(userId?: number) {
  return request.get('/v1/tasks/my-tasks/count', { params: { userId } })
}

export function createTask(data: {
  projectId: number
  taskType: string
  title: string
  description?: string
  priority?: string
  assigneeId?: number
  bizType?: string
  bizId?: number
  dueDate?: string
}) {
  return request.post('/v1/tasks', data)
}

export function updateTask(id: number, data: {
  title?: string
  description?: string
  status?: string
  priority?: string
  assigneeId?: number
  bizType?: string
  bizId?: number
  dueDate?: string
}) {
  return request.post(`/v1/tasks/${id}`, data)
}

export function deleteTask(id: number) {
  return request.post(`/v1/tasks/${id}/delete`)
}
