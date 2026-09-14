/**
 * @author HXN
 * @date 2026-08-30
 * @description 变更记录模块 API
 */
import request from './request'

export interface ChangeLogItem {
  id: number
  bizType: string
  bizId: number
  fieldName: string
  oldValue: string | null
  newValue: string | null
  createdBy: number | null
  createdByName: string | null
  createdAt: string
}

/** 查询业务对象下的变更记录列表 */
export function getChangeLogs(bizType: string, bizId: number, fieldName?: string) {
  return request.get('/v1/change-logs', { params: { bizType, bizId, fieldName } })
}
