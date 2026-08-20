<script setup lang="ts">
/**
 * Action 调试 - M7
 * 选择环境 + 输入参数 → 执行 Action → 查看步骤执行结果
 */
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { getAction, debugAction } from '@/api/action'
import { getEnvironments } from '@/api/environment'

const route = useRoute()
const router = useRouter()
const projectId = computed(() => Number(route.params.id))
const actionId = computed(() => Number(route.params.actionId))

const action = ref<any>(null)
const environments = ref<any[]>([])
const selectedEnvId = ref<number>(0)
const inputParams = ref('{}')
const debugResult = ref<any>(null)
const loading = ref(false)
const executing = ref(false)

const statusLabels: Record<string, string> = {
  PASSED: '通过', FAILED: '失败', SKIPPED: '跳过', ERROR: '错误', PENDING: '等待中',
}

async function loadData() {
  loading.value = true
  try {
    const [actionRes, envRes] = await Promise.all([
      getAction(projectId.value, actionId.value),
      getEnvironments(projectId.value),
    ])
    action.value = (actionRes as any).data || {}
    environments.value = (envRes as any).data || []
  } catch { message.error('加载数据失败') } finally { loading.value = false }
}

function formatInput() {
  try {
    inputParams.value = JSON.stringify(JSON.parse(inputParams.value || '{}'), null, 2)
  } catch {
    message.warning('JSON 格式错误')
  }
}

async function handleDebug() {
  if (!selectedEnvId.value) { message.warning('请选择执行环境'); return }
  let params: Record<string, any>
  try {
    params = JSON.parse(inputParams.value || '{}')
  } catch {
    message.warning('输入参数不是有效的 JSON')
    return
  }
  executing.value = true
  debugResult.value = null
  try {
    const res: any = await debugAction(projectId.value, actionId.value, {
      environmentId: selectedEnvId.value,
      inputParams: params,
    })
    debugResult.value = res.data
    if (debugResult.value?.success) {
      message.success('调试执行完成')
    } else {
      message.error('调试执行失败')
    }
  } catch (e: any) {
    message.error(e?.response?.data?.message || '调试失败')
  } finally { executing.value = false }
}

onMounted(loadData)
</script>

<template>
  <div v-if="loading" style="text-align:center;padding:60px"><a-spin size="large" /></div>
  <div v-else>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <div style="display:flex;align-items:center;gap:12px">
        <a @click="router.back()">← 返回</a>
        <h2 style="margin:0">Action 调试: {{ action?.name || '' }}</h2>
      </div>
    </div>

    <a-row :gutter="16">
      <!-- 左侧：配置区 -->
      <a-col :span="10">
        <a-card title="调试配置" size="small">
          <a-form layout="vertical">
            <a-form-item label="执行环境" required>
              <a-select v-model:value="selectedEnvId" placeholder="选择环境">
                <a-select-option v-for="env in environments" :key="env.id" :value="env.id">
                  <a-tag :color="env.isCurrent ? 'green' : 'default'" size="small">{{ env.isCurrent ? '当前' : '' }}</a-tag>
                  {{ env.name }} ({{ env.host }}:{{ env.port }})
                </a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item>
              <template #label>
                <div style="display:flex;justify-content:space-between;align-items:center;width:100%">
                  <span>输入参数 (JSON)</span>
                  <a-button size="small" @click="formatInput">格式化</a-button>
                </div>
              </template>
              <a-textarea v-model:value="inputParams" :rows="10" style="font-family:monospace;font-size:12px"
                placeholder='{"key":"value"}' />
            </a-form-item>
            <a-button type="primary" block :loading="executing" @click="handleDebug">执行调试</a-button>
          </a-form>
        </a-card>

        <a-card v-if="action?.description" title="Action 说明" size="small" style="margin-top:16px">
          <p style="color:#666;margin:0">{{ action.description }}</p>
        </a-card>
      </a-col>

      <!-- 右侧：结果区 -->
      <a-col :span="14">
        <a-card title="执行结果" size="small">
          <div v-if="!debugResult" style="text-align:center;padding:40px;color:#999">
            点击「执行调试」查看结果
          </div>
          <div v-else>
            <!-- 概览 -->
            <a-alert
              :type="debugResult.success ? 'success' : 'error'"
              :message="debugResult.success ? '执行成功' : '执行失败'"
              :description="debugResult.errorMessage || debugResult.message || ''"
              show-icon
              style="margin-bottom:16px"
            />

            <!-- 步骤结果 -->
            <div v-if="debugResult.stepResults?.length" style="margin-bottom:16px">
              <div style="font-weight:600;margin-bottom:8px">步骤执行明细</div>
              <a-timeline>
                <a-timeline-item v-for="(step, idx) in debugResult.stepResults" :key="idx"
                  :color="step.status === 'PASSED' ? 'green' : step.status === 'FAILED' || step.status === 'ERROR' ? 'red' : 'gray'">
                  <div style="font-weight:600">
                    {{ step.stepName || step.name }} - {{ statusLabels[step.status] || step.status }}
                  </div>
                  <div v-if="step.message" style="color:#999;font-size:12px">{{ step.message }}</div>
                  <div v-if="step.durationMs != null" style="color:#999;font-size:12px">耗时: {{ step.durationMs }}ms</div>
                  <details v-if="step.response" style="margin-top:4px">
                    <summary style="cursor:pointer;font-size:12px;color:#1890ff">响应详情</summary>
                    <pre style="font-size:11px;background:#f5f5f5;padding:8px;max-height:200px;overflow:auto">{{ JSON.stringify(step.response, null, 2) }}</pre>
                  </details>
                </a-timeline-item>
              </a-timeline>
            </div>

            <!-- 输出 -->
            <div v-if="debugResult.output != null">
              <div style="font-weight:600;margin-bottom:8px">输出</div>
              <pre style="background:#f5f5f5;padding:12px;border-radius:4px;font-size:12px;max-height:300px;overflow:auto">{{ typeof debugResult.output === 'string' ? debugResult.output : JSON.stringify(debugResult.output, null, 2) }}</pre>
            </div>
          </div>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>
