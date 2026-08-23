<!--
 @author HXN
 @date 2026-08-20 15:34
 @description 执行详情视图
-->
<script setup lang="ts">
/**
 * 执行详情 - M9
 * 统计卡片 + 用例结果列表 + 日志查看
 */
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getExecution, getExecutionResults, cancelExecution } from '@/api/execution'
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
      progressPercent: progress.value.progressPercent,
      passRate: progress.value.passRate,
      currentCaseName: progress.value.currentCaseName,
    }
  }
  return execution.value
})

const liveMessage = computed(() => progress.value?.message || '')
const liveProgressPercent = computed(() => progress.value?.progressPercent || 0)
const livePassRate = computed(() => progress.value?.passRate || 0)
const liveCurrentCase = computed(() => progress.value?.currentCaseName || '')

const statusTypeMap: Record<string, string> = {
  PENDING: 'info', QUEUED: 'info', RUNNING: '', COMPLETED: 'success',
  FAILED: 'danger', CANCELLED: 'warning',
  PASSED: 'success', SKIPPED: 'info', ERROR: 'danger',
}
const statusLabels: Record<string, string> = {
  PENDING: '等待中', QUEUED: '排队中', RUNNING: '执行中', COMPLETED: '已完成',
  FAILED: '执行失败', CANCELLED: '已取消',
  PASSED: '通过', SKIPPED: '跳过', ERROR: '错误',
}

async function loadData() {
  loading.value = true
  try {
    const [execRes, resultsRes] = await Promise.all([
      getExecution(executionId.value),
      getExecutionResults(executionId.value),
    ])
    execution.value = (execRes as any).data || {}
    results.value = (resultsRes as any).data || []
    // 执行中或排队中则建立 WebSocket 连接
    if (execution.value.status === 'RUNNING' || execution.value.status === 'PENDING' || execution.value.status === 'QUEUED') {
      connect()
    }
  } catch { ElMessage.error('加载执行详情失败') } finally { loading.value = false }
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

async function handleCancel() {
  try {
    await cancelExecution(executionId.value)
    ElMessage.success('已取消执行')
    loadData()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '取消失败')
  }
}

const canCancel = computed(() => {
  const s = liveExecution.value?.status
  return s === 'RUNNING' || s === 'PENDING' || s === 'QUEUED'
})

onMounted(loadData)
</script>

<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <div style="display:flex;align-items:center;gap:12px">
        <h2 style="margin:0">执行详情</h2>
        <el-tag v-if="connected" type="success" size="small" effect="dark">实时</el-tag>
      </div>
      <div style="display:flex;gap:8px">
        <el-button @click="router.back()">返回</el-button>
        <el-button @click="refresh">刷新</el-button>
        <el-button v-if="canCancel" type="danger" @click="handleCancel">取消执行</el-button>
      </div>
    </div>

    <!-- 实时进度提示 -->
    <el-alert v-if="liveMessage && connected" :title="liveMessage" type="info" show-icon
      :closable="false" style="margin-bottom:16px" />

    <!-- 进度条 -->
    <el-card v-if="connected && liveExecution.status === 'RUNNING'" style="margin-bottom:16px">
      <div style="display:flex;align-items:center;gap:16px">
        <el-progress :percentage="liveProgressPercent" :stroke-width="20" text-inside striped striped-flow style="flex:1" />
        <div style="white-space:nowrap;font-size:13px;color:#606266">
          <span v-if="liveCurrentCase" style="margin-right:12px">当前: {{ liveCurrentCase }}</span>
          <span>通过率: {{ livePassRate }}%</span>
        </div>
      </div>
    </el-card>

    <!-- 统计卡片 -->
    <el-row :gutter="16" style="margin-bottom:16px">
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">总用例</div>
          <div class="stat-value">{{ liveExecution.totalCases || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">通过</div>
          <div class="stat-value" style="color:#67c23a">{{ liveExecution.passedCases || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">失败</div>
          <div class="stat-value" style="color:#f56c6c">{{ liveExecution.failedCases || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">跳过</div>
          <div class="stat-value" style="color:#909399">{{ liveExecution.skippedCases || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">耗时(ms)</div>
          <div class="stat-value">{{ liveExecution.durationMs || 0 }}</div>
        </el-card>
      </el-col>
      <el-col :span="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">状态</div>
          <div style="margin-top:4px">
            <el-tag :type="(statusTypeMap[liveExecution.status] || 'info') as any" size="large">
              {{ statusLabels[liveExecution.status] || liveExecution.status }}
            </el-tag>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 基本信息 -->
    <el-card style="margin-bottom:16px">
      <el-descriptions :column="3" size="small" border>
        <el-descriptions-item label="计划">{{ execution.planName }}</el-descriptions-item>
        <el-descriptions-item label="环境">{{ execution.environmentName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="触发方式">{{ execution.triggerType }}</el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ execution.startedAt?.substring(0, 19).replace('T', ' ') || '-' }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ execution.finishedAt?.substring(0, 19).replace('T', ' ') || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ execution.createdAt?.substring(0, 19).replace('T', ' ') || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 用例结果列表 -->
    <el-table v-loading="loading" :data="results" row-key="id" border style="width:100%">
      <el-table-column prop="caseName" label="用例名称" width="200" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="(statusTypeMap[row.status] || 'info') as any" size="small">{{ statusLabels[row.status] || row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="durationMs" label="耗时(ms)" width="100" />
      <el-table-column prop="actualResult" label="结果" show-overflow-tooltip />
      <el-table-column label="操作" width="80">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="showLogs(row)">日志</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 日志弹窗 -->
    <el-dialog v-model="logModalVisible" title="执行日志" width="800px">
      <el-timeline v-if="currentLogs.length > 0">
        <el-timeline-item v-for="(log, idx) in currentLogs" :key="idx"
          :type="log.status === 'PASSED' ? 'success' : log.status === 'FAILED' ? 'danger' : 'info'"
          :hollow="log.status !== 'PASSED'">
          <div style="font-weight:600">{{ log.stepName }} [{{ log.phase }}] - {{ log.status }}</div>
          <div style="color:#909399;font-size:12px">{{ log.message }}</div>
          <div v-if="log.assertionSummary" style="color:#409eff;font-size:12px;margin-top:2px">{{ log.assertionSummary }}</div>
          <details v-if="log.request" style="margin-top:4px">
            <summary style="cursor:pointer;font-size:12px;color:#409eff">请求详情</summary>
            <pre style="font-size:11px;background:#f5f7fa;padding:8px;max-height:200px;overflow:auto">{{ JSON.stringify(log.request, null, 2) }}</pre>
          </details>
          <details v-if="log.response" style="margin-top:4px">
            <summary style="cursor:pointer;font-size:12px;color:#409eff">响应详情</summary>
            <pre style="font-size:11px;background:#f5f7fa;padding:8px;max-height:200px;overflow:auto">{{ JSON.stringify(log.response, null, 2) }}</pre>
          </details>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="无日志" />
    </el-dialog>
  </div>
</template>

<style scoped>
.stat-card { text-align:center; }
.stat-label { font-size:12px; color:#909399; margin-bottom:4px; }
.stat-value { font-size:24px; font-weight:600; color:#303133; }
</style>
