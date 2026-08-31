/**
 * @author HXN
 * @date 2026-08-30
 * @description 需求文档模块 API
 */
import request from './request'

// ===== 类型定义 =====

export interface RequirementVersion {
  id: number
  projectId: number
  versionName: string
  description: string | null
  status: string
  startDate: string | null
  endDate: string | null
  itemCount: number
  createdAt: string
  updatedAt: string
}

export interface RequirementItem {
  id: number
  versionId: number
  title: string
  description: string | null
  reqType: string
  priority: string
  status: string
  assignee: string | null
  deadline: string | null
  sortOrder: number
  createdAt: string
  updatedAt: string
}

export interface RequirementVersionCreateRequest {
  versionName: string
  description?: string
  status?: string
  startDate?: string
  endDate?: string
}

export interface RequirementItemCreateRequest {
  title: string
  description?: string
  reqType?: string
  priority?: string
  status?: string
  assignee?: string
  deadline?: string
}

// ===== 版本 API =====

/** 查询项目下的版本列表 */
export function getRequirementVersions(projectId: number) {
  return request.get(`/v1/projects/${projectId}/requirement-versions`)
}

/** 创建版本 */
export function createRequirementVersion(projectId: number, data: RequirementVersionCreateRequest) {
  return request.post(`/v1/projects/${projectId}/requirement-versions`, data)
}

/** 更新版本 */
export function updateRequirementVersion(versionId: number, data: RequirementVersionCreateRequest) {
  return request.post(`/v1/requirement-versions/${versionId}`, data)
}

/** 删除版本 */
export function deleteRequirementVersion(versionId: number) {
  return request.post(`/v1/requirement-versions/${versionId}/delete`)
}

// ===== 需求条目 API =====

/** 查询单个需求条目详情 */
export function getRequirementItem(itemId: number) {
  return request.get(`/v1/requirement-items/${itemId}`)
}

/** 查询版本下的需求条目列表 */
export function getRequirementItems(versionId: number) {
  return request.get(`/v1/requirement-versions/${versionId}/items`)
}

/** 创建需求条目 */
export function createRequirementItem(versionId: number, data: RequirementItemCreateRequest) {
  return request.post(`/v1/requirement-versions/${versionId}/items`, data)
}

/** 更新需求条目 */
export function updateRequirementItem(itemId: number, data: RequirementItemCreateRequest) {
  return request.post(`/v1/requirement-items/${itemId}`, data)
}

/** 删除需求条目 */
export function deleteRequirementItem(itemId: number) {
  return request.post(`/v1/requirement-items/${itemId}/delete`)
}
