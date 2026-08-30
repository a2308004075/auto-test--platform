/**
 * @author HXN
 * @date 2026-08-30
 * @description 前端错误日志上报工具
 *
 * 将前端运行时错误发送到后端，写入 backend/log/frontend-err.log。
 * 使用原生 fetch 而非项目 Axios 实例，避免与 Axios 拦截器形成循环依赖。
 */

/** 错误类型枚举 */
export type ErrorType = 'vue_error' | 'js_error' | 'promise_rejection' | 'api_error'

/** 日志上报请求体（与后端 FrontendLogRequest 对齐） */
interface LogPayload {
  type: ErrorType
  message: string
  stack?: string
  url?: string
  userAgent?: string
  extra?: string
}

/** 后端日志上报接口地址 */
const LOG_ENDPOINT = '/api/v1/frontend-log'

/**
 * 上报前端错误日志到后端
 *
 * 使用 fetch + keepalive 确保在页面卸载等场景下也能发出请求。
 * 上报失败时静默忽略，不影响业务流程。
 */
export function reportError(
  type: ErrorType,
  message: string,
  options?: { stack?: string; extra?: string },
): void {
  const payload: LogPayload = {
    type,
    message,
    url: window.location.href,
    userAgent: navigator.userAgent,
    stack: options?.stack,
    extra: options?.extra,
  }

  const body = JSON.stringify(payload)

  // 优先使用 sendBeacon（页面卸载场景可靠发送），不可用时降级为 fetch
  if (navigator.sendBeacon) {
    const blob = new Blob([body], { type: 'application/json' })
    const sent = navigator.sendBeacon(LOG_ENDPOINT, blob)
    if (sent) return
  }

  // 降级：fetch + keepalive
  fetch(LOG_ENDPOINT, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body,
    keepalive: true,
  }).catch(() => {
    // 上报失败静默忽略
  })
}

/**
 * 注册全局错误监听（Vue errorHandler、window.onerror、unhandledrejection）
 * 在 main.ts 中调用一次即可
 */
export function setupGlobalErrorHandlers(): void {
  // 全局 JS 运行时错误
  window.onerror = (message, _source, _lineno, _colno, error) => {
    reportError('js_error', String(message), {
      stack: error?.stack,
    })
  }

  // 未捕获的 Promise 异常
  window.addEventListener('unhandledrejection', (event: PromiseRejectionEvent) => {
    const reason = event.reason
    const message = reason?.message || String(reason)
    const stack = reason?.stack

    // 避免重复上报 Axios 拦截器已处理的错误（Axios 会自行调用 reportError）
    if (reason?.__logged) return

    reportError('promise_rejection', message, { stack })
  })
}
