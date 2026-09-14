/**
 * @author HXN
 * @date 2026-08-20 15:34
 * @description 系统设置模块 API
 */
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

/**
 * 测试 SMTP 邮件发送
 */
export function testSmtpSend(data: { recipient: string; content: string }) {
  return request.post('/v1/settings/test-smtp', data)
}

/**
 * 测试 Webhook 通知发送
 */
export function testWebhookSend(data: { recipient?: string; content: string }) {
  return request.post('/v1/settings/test-webhook', data)
}
