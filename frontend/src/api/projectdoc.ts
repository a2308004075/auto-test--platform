/**
 * @author HXN
 * @date 2026-08-30
 * @description 项目文档模块 API
 */
import request from './request'

/**
 * 项目文档模块 API
 */

// ===== 类型定义 =====

export interface ProjectDocGroup {
  id: number
  projectId: number
  parentId: number | null
  name: string
  description?: string
  isSystem: number
  docCount?: number
  createdAt?: string
}

export interface ProjectDoc {
  id: number
  projectId: number
  groupId: number | null
  docName: string
  fileName: string
  fileSize: number
  contentType?: string
  description?: string
  createdAt?: string
  updatedAt?: string
}

// ===== 文档 API =====

export function getProjectDocs(projectId: number, params?: any) {
  return request.get(`/v1/projects/${projectId}/docs`, { params })
}

export function uploadProjectDoc(projectId: number, form: FormData) {
  // 大文件上传单独放宽超时（10 分钟）
  return request.post(`/v1/projects/${projectId}/docs/upload`, form, {
    timeout: 600000,
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function updateProjectDoc(docId: number, data: { docName: string; description?: string; groupId?: number }) {
  return request.post(`/v1/docs/${docId}`, data)
}

export function replaceProjectDoc(docId: number, form: FormData) {
  // 大文件替换单独放宽超时（10 分钟）
  return request.post(`/v1/docs/${docId}/replace`, form, {
    timeout: 600000,
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function deleteProjectDoc(docId: number) {
  return request.post(`/v1/docs/${docId}/delete`)
}

export async function downloadProjectDoc(docId: number): Promise<Blob> {
  try {
    const res: any = await request.get(`/v1/docs/${docId}/download`, { responseType: 'blob' })
    return res as Blob
  } catch (e: any) {
    // 错误响应体为 blob（JSON），读取后抛出后端 message
    const blob: Blob | undefined = e?.response?.data
    if (blob instanceof Blob && blob.size > 0) {
      const text = await blob.text().catch(() => '')
      let message = ''
      try { message = JSON.parse(text)?.message || '' } catch { /* 非 JSON 错误体 */ }
      if (message) throw new Error(message)
    }
    throw e
  }
}

// ===== 文档分组 API =====

export function getDocGroups(projectId: number) {
  return request.get(`/v1/projects/${projectId}/doc-groups`)
}

export function createDocGroup(projectId: number, data: { parentId?: number | null; name: string; description?: string }) {
  return request.post(`/v1/projects/${projectId}/doc-groups`, data)
}

export function updateDocGroup(projectId: number, groupId: number, data: { parentId?: number | null; name?: string; description?: string }) {
  return request.post(`/v1/projects/${projectId}/doc-groups/${groupId}`, data)
}

export function deleteDocGroup(projectId: number, groupId: number) {
  return request.post(`/v1/projects/${projectId}/doc-groups/${groupId}/delete`)
}
