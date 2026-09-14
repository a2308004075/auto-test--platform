/**
 * @author HXN
 * @date 2026-08-22 13:28
 * @description 缓存模块 API
 */
import request from './request'

/**
 * 缓存管理模块 API
 */

export interface CacheItem {
  key: string
  value: string
  ttl: number
}

export interface CacheSetRequest {
  key: string
  value: string
  ttl?: number
}

/** 精确查询缓存 */
export function getCacheByKey(key: string) {
  return request.get(`/v1/sys/cache/${encodeURIComponent(key)}`)
}

/** 模糊搜索缓存键 */
export function searchCache(pattern: string, limit?: number) {
  return request.get('/v1/sys/cache/search', { params: { pattern, limit: limit || 50 } })
}

/** 设置缓存 */
export function setCache(data: CacheSetRequest) {
  return request.post('/v1/sys/cache', data)
}

/** 删除缓存 */
export function deleteCache(key: string) {
  return request.delete(`/v1/sys/cache/${encodeURIComponent(key)}`)
}
