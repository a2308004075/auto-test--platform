import { ref, onUnmounted, type Ref } from 'vue'

export interface ExecutionProgress {
  type: string
  status: string
  totalCases: number
  passedCases: number
  failedCases: number
  skippedCases: number
  durationMs: number
  message: string
}

/**
 * 执行进度 WebSocket 连接
 *
 * 连接 /ws/execution/{executionId}，实时接收执行进度推送。
 * 当收到 PROGRESS 消息时更新 progress ref，收到终态时自动断开。
 */
export function useExecutionWebSocket(executionId: Ref<number>) {
  const progress = ref<ExecutionProgress | null>(null)
  const connected = ref(false)
  let ws: WebSocket | null = null
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null

  function getWsUrl() {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = window.location.host
    return `${protocol}//${host}/ws/execution/${executionId.value}`
  }

  function connect() {
    if (!executionId.value || ws) return
    try {
      ws = new WebSocket(getWsUrl())
    } catch {
      return
    }

    ws.onopen = () => {
      connected.value = true
    }

    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data) as ExecutionProgress
        progress.value = data
        // 终态自动断开
        if (data.status === 'COMPLETED' || data.status === 'FAILED' || data.status === 'CANCELLED') {
          close()
        }
      } catch {
        // ignore parse errors
      }
    }

    ws.onclose = () => {
      connected.value = false
      ws = null
      // 非终态时尝试重连
      if (progress.value && !['COMPLETED', 'FAILED', 'CANCELLED'].includes(progress.value.status)) {
        reconnectTimer = setTimeout(connect, 3000)
      }
    }

    ws.onerror = () => {
      connected.value = false
    }
  }

  function close() {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    if (ws) {
      ws.onclose = null
      ws.close()
      ws = null
    }
    connected.value = false
  }

  onUnmounted(close)

  return { progress, connected, connect, close }
}
