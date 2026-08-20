<script setup lang="ts">
/**
 * 执行详情 - M9
 * 统计卡片 + 用例结果列表 + 日志查看
 */
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { getExecution, getExecutionResults } from '@/api/execution'
import { useExecutionWebSocket } from '@/composables/useExecutionWebSocket'

const route = useRoute()
const router = useRouter()
const executionId = computed(() => Number(route.params.executionId))

const execution = ref<any>({})
const results = ref<any[]>([])
const loading = ref(false)
const logModalVisible = ref(false)
const currentLogs = ref<any[]>([])

// WebSocket 实时进度
const { progress, connected, connect } = useExecutionWebSocket(executionId)

// 合并 WebSocket 实时进度到执行数据
const liveExecution = computed(() => {
  if (progress.value) {
    return {
      ...execution.value,
      status: progress.value.status,
      totalCases: progress.value.totalCases,
      passedCases: progress.value.passedCases,
      failedCases: progress.value.failedCases,
      skippedCases: progress.value.skippedCases,
      durationMs: progress.value.durationMs,
    }
  }
  return execution.value
})

const liveMessage = computed(() => progress.value?.message || '')

const statusColors: Record<string, string> = {
  PENDING: 'default', RUNNING: 'processing', COMPLETED: 'success',
  FAILED: 'error', CANCELLED: 'warning',
  PASSED: 'success', SKIPPED: 'default', ERROR: 'error',
}
const statusLabels: Record<string, string> = {
  PENDING: '等待中', RUNNING: '执行中', COMPLETED: '已完成',
  FAILED: '执行失败', CANCELLED: '已取消',
  PASSED: '通过', SKIPPED: '跳过', ERROR: '错误',
}

const resultColumns = [
  { title: '用例名称', dataIndex: 'caseName', width: 200 },
  { title: '状态', key: 'status', width: 90 },
  { title: '耗时(ms)', dataIndex: 'durationMs', width: 100 },
  { title: '结果', dataIndex: 'actualResult', ellipsis: true },
  { title: '操作', key: 'action', width: 80 },
]

async function loadData() {
  loading.value = true
  try {
    const [execRes, resultsRes] = await Promise.all([
      getExecution(executionId.value),
      getExecutionResults(executionId.value),
    ])
    execution.value = (execRes as any).data || {}
    results.value = (resultsRes as any).data || []
    // 执行中则建立 WebSocket 连接
    if (execution.value.status === 'RUNNING' || execution.value.status === 'PENDING') {
      connect()
    }
  } catch { message.error('加载执行详情失败') } finally { loading.value = false }
}

// 监听 WebSocket 进度，终态时自动刷新结果
watch(() => progress.value?.status, (status) => {
  if (status === 'COMPLETED' || status === 'FAILED' || status === 'CANCELLED') {
    loadData()
  }
})

function showLogs(record: any) {
  try {
    currentLogs.value = record.logs ? JSON.parse(record.logs) : []
  } catch {
    currentLogs.value = []
  }
  logModalVisible.value = true
}

function refresh() { loadData() }

onMounted(loadData)
</script>

<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <div style="display:flex;align-items:center;gap:12px">
        <h2 style="margin:0">执行详情</h2>
        <a-badge v-if="connected" status="processing" text="实时" />
      </div>
      <a-space>
        <a-button @click="router.back()">返回</a-button>
        <a-button @click="refresh">刷新</a-button>
      </a-space>
    </div>

    <!-- 实时进度提示 -->
    <a-alert v-if="liveMessage && connected" :message="liveMessage" type="info" show-icon
      style="margin-bottom:16px" />

    <!-- 统计卡片 -->
    <a-row :gutter="16" style="margin-bottom:16px">
      <a-col :span="4">
        <a-card size="small">
          <a-statistic title="总用例" :value="liveExecution.totalCases || 0" />
        </a-card>
      </a-col>
      <a-col :span="4">
        <a-card size="small">
          <a-statistic title="通过" :value="liveExecution.passedCases || 0" :value-style="{ color: '#52c41a' }" />
        </a-card>
      </a-col>
      <a-col :span="4">
        <a-card size="small">
          <a-statistic title="失败" :value="liveExecution.failedCases || 0" :value-style="{ color: '#ff4d4f' }" />
        </a-card>
      </a-col>
      <a-col :span="4">
        <a-card size="small">
          <a-statistic title="跳过" :value="liveExecution.skippedCases || 0" :value-style="{ color: '#999' }" />
        </a-card>
      </a-col>
      <a-col :span="4">
        <a-card size="small">
          <a-statistic title="耗时(ms)" :value="liveExecution.durationMs || 0" />
        </a-card>
      </a-col>
      <a-col :span="4">
        <a-card size="small">
          <div style="font-size:12px;color:#999;margin-bottom:4px">状态</div>
          <a-tag :color="statusColors[liveExecution.status] || 'default'" style="font-size:14px">
            {{ statusLabels[liveExecution.status] || liveExecution.status }}
          </a-tag>
        </a-card>
      </a-col>
    </a-row>

    <!-- 基本信息 -->
    <a-card size="small" style="margin-bottom:16px">
      <a-descriptions :column="3" size="small">
        <a-descriptions-item label="计划">{{ execution.planName }}</a-descriptions-item>
        <a-descriptions-item label="环境">{{ execution.environmentName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="触发方式">{{ execution.triggerType }}</a-descriptions-item>
        <a-descriptions-item label="开始时间">{{ execution.startedAt?.substring(0, 19).replace('T', ' ') || '-' }}</a-descriptions-item>
        <a-descriptions-item label="结束时间">{{ execution.finishedAt?.substring(0, 19).replace('T', ' ') || '-' }}</a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ execution.createdAt?.substring(0, 19).replace('T', ' ') || '-' }}</a-descriptions-item>
      </a-descriptions>
    </a-card>

    <!-- 用例结果列表 -->
    <a-table :columns="resultColumns" :data-source="results" :loading="loading" row-key="id" size="middle"
      :pagination="false">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag :color="statusColors[record.status] || 'default'">{{ statusLabels[record.status] || record.status }}</a-tag>
        </template>
        <template v-if="column.key === 'action'">
          <a @click="showLogs(record)">日志</a>
        </template>
      </template>
    </a-table>

    <!-- 日志弹窗 -->
    <a-modal v-model:open="logModalVisible" title="执行日志" width="800px" :footer="null">
      <a-timeline>
        <a-timeline-item v-for="(log, idx) in currentLogs" :key="idx"
          :color="log.status === 'PASSED' ? 'green' : log.status === 'FAILED' ? 'red' : 'gray'">
          <div style="font-weight:600">{{ log.stepName }} [{{ log.phase }}] - {{ log.status }}</div>
          <div style="color:#999;font-size:12px">{{ log.message }}</div>
          <div v-if="log.assertionSummary" style="color:#1890ff;font-size:12px;margin-top:2px">{{ log.assertionSummary }}</div>
          <details v-if="log.request" style="margin-top:4px">
            <summary style="cursor:pointer;font-size:12px;color:#1890ff">请求详情</summary>
            <pre style="font-size:11px;background:#f5f5f5;padding:8px;max-height:200px;overflow:auto">{{ JSON.stringify(log.request, null, 2) }}</pre>
          </details>
          <details v-if="log.response" style="margin-top:4px">
            <summary style="cursor:pointer;font-size:12px;color:#1890ff">响应详情</summary>
            <pre style="font-size:11px;background:#f5f5f5;padding:8px;max-height:200px;overflow:auto">{{ JSON.stringify(log.response, null, 2) }}</pre>
          </details>
        </a-timeline-item>
      </a-timeline>
      <a-empty v-if="currentLogs.length === 0" description="无日志" />
    </a-modal>
  </div>
</template>
