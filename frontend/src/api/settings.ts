import request from './request'

/**
 * 全局配置模块 API（M1）
 */

export interface GlobalConfigItem {
  id: number
  configKey: string
  configValue: string
  description?: string
  updatedAt?: string
}

export function getSettings() {
  return request.get('/v1/settings')
}

export function getSetting(configKey: string) {
  return request.get(`/v1/settings/${configKey}`)
}

export function updateSetting(configKey: string, data: { configValue: string; description?: string }) {
  return request.post(`/v1/settings/${configKey}`, data)
}
