/**
 * @author HXN
 * @date 2026-08-22 13:28
 * @description 字典模块 API
 */
import request from './request'

/**
 * 字典管理模块 API
 */

export interface DictListItem {
  id: number
  dictType: string
  dictTypeName: string
  dictValue: string
  dictValueName: string
  sortNo: number
  remark?: string
  createdAt: string
  updatedAt: string
}

export interface DictCreateRequest {
  dictType: string
  dictTypeName: string
  dictValue: string
  dictValueName: string
  sortNo?: number
  remark?: string
}

export interface DictPageResponse {
  items: DictListItem[]
  total: number
  page: number
  page_size: number
}

/** 分页查询字典列表 */
export function getDictPage(params: {
  dictType?: string
  dictTypeName?: string
  page?: number
  pageSize?: number
}) {
  return request.get('/v1/sys/dicts', { params })
}

/** 获取单个字典 */
export function getDict(id: number) {
  return request.get(`/v1/sys/dicts/${id}`)
}

/** 新增字典 */
export function addDict(data: DictCreateRequest) {
  return request.post('/v1/sys/dicts', data)
}

/** 更新字典 */
export function updateDict(id: number, data: DictCreateRequest) {
  return request.post(`/v1/sys/dicts/${id}`, data)
}

/** 批量删除字典 */
export function batchDeleteDict(ids: number[]) {
  return request.delete('/v1/sys/dicts/batch', { data: { ids } })
}

/** 根据字典类型查询字典值列表 */
export function getDictByType(dictType: string) {
  return request.get(`/v1/sys/dicts/type/${dictType}`)
}

/** 导出字典 Excel */
export function exportDicts() {
  return request.get('/v1/sys/dicts/export', { responseType: 'blob' })
}

/** 导入字典 Excel */
export function importDicts(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/v1/sys/dicts/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
