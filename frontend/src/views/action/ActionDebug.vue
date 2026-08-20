<script setup lang="ts">
/**
 * Action 调试 - M7
 * 选择环境 + 输入参数 → 执行 Action → 查看步骤执行结果
 */
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
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
  } catch { ElMessage.error('加载数据失败') } finally { loading.value = false }
}

function formatInput() {
  try {
    inputParams.value = JSON.stringify(JSON.parse(inputParams.value || '{}'), null, 2)
  } catch {
    ElMessage.warning('JSON 格式错误')
  }
}

async function handleDebug() {
  if (!selectedEnvId.value) { ElMessage.warning('请选择执行环境'); return }
  let params: Record<string, any>
  try {
    params = JSON.parse(inputParams.value || '{}')
  } catch {
    ElMessage.warning('输入参数不是有效的 JSON')
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
      ElMessage.success('调试执行成功')
    } else {
      ElMessage.error('调试执行失败')
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '调试失败')
  } finally { executing.value = false }
}

onMounted(loadData)
</script>

<template>
  <div v-loading="loading">
    <div v-if="!loading">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
        <div style="display:flex;align-items:center;gap:12px">
          <el-button type="primary" link @click="router.back()">← 返回</el-button>
          <h2 style="margin:0">Action 调试: {{ action?.name || '' }}</h2>
        </div>
      </div>

      <el-row :gutter="16">
        <!-- 左侧：配置区 -->
        <el-col :span="10">
          <el-card>
            <template #header><span>调试配置</span></template>
            <el-form label-position="top">
              <el-form-item label="执行环境" required>
                <el-select v-model="selectedEnvId" placeholder="选择环境" style="width:100%">
                  <el-option v-for="env in environments" :key="env.id" :value="env.id"
                    :label="`${env.name} (${env.host}:${env.port})${env.isCurrent ? ' [当前]' : ''}`" />
                </el-select>
              </el-form-item>
              <el-form-item>
                <template #label>
                  <div style="display:flex;justify-content:space-between;align-items:center;width:100%">
                    <span>输入参数 (JSON)</span>
                    <el-button size="small" @click="formatInput">格式化</el-button>
                  </div>
                </template>
                <el-input v-model="inputParams" type="textarea" :rows="10" style="font-family:monospace;font-size:12px"
                  placeholder='{"key":"value"}' />
              </el-form-item>
              <el-button type="primary" :loading="executing" @click="handleDebug" style="width:100%">执行调试</el-button>
            </el-form>
          </el-card>

          <el-card v-if="action?.description" style="margin-top:16px">
            <template #header><span>Action 说明</span></template>
            <p style="color:#606266;margin:0">{{ action.description }}</p>
          </el-card>
        </el-col>

        <!-- 右侧：结果区 -->
        <el-col :span="14">
          <el-card>
            <template #header><span>执行结果</span></template>
            <div v-if="!debugResult" style="text-align:center;padding:40px;color:#909399">
              点击「执行调试」查看结果
            </div>
            <div v-else>
              <!-- 概览 -->
              <el-alert
                :type="debugResult.success ? 'success' : 'error'"
                :title="debugResult.success ? '执行成功' : '执行失败'"
                :description="debugResult.errorMessage || debugResult.message || ''"
                show-icon
                :closable="false"
                style="margin-bottom:16px"
              />

              <!-- 步骤结果 -->
              <div v-if="debugResult.stepResults?.length" style="margin-bottom:16px">
                <div style="font-weight:600;margin-bottom:8px">步骤执行明细</div>
                <el-timeline>
                  <el-timeline-item v-for="(step, idx) in debugResult.stepResults" :key="idx"
                    :type="step.status === 'PASSED' ? 'success' : step.status === 'FAILED' || step.status === 'ERROR' ? 'danger' : 'info'"
                    :hollow="step.status !== 'PASSED'">
                    <div style="font-weight:600">
                      {{ step.stepName || step.name }} - {{ statusLabels[step.status] || step.status }}
                    </div>
                    <div v-if="step.message" style="color:#909399;font-size:12px">{{ step.message }}</div>
                    <div v-if="step.durationMs != null" style="color:#909399;font-size:12px">耗时: {{ step.durationMs }}ms</div>
                    <details v-if="step.response" style="margin-top:4px">
                      <summary style="cursor:pointer;font-size:12px;color:#409eff">响应详情</summary>
                      <pre style="font-size:11px;background:#f5f7fa;padding:8px;max-height:200px;overflow:auto">{{ JSON.stringify(step.response, null, 2) }}</pre>
                    </details>
                  </el-timeline-item>
                </el-timeline>
              </div>

              <!-- 输出 -->
              <div v-if="debugResult.output != null">
                <div style="font-weight:600;margin-bottom:8px">输出</div>
                <pre style="background:#f5f7fa;padding:12px;border-radius:4px;font-size:12px;max-height:300px;overflow:auto">{{ typeof debugResult.output === 'string' ? debugResult.output : JSON.stringify(debugResult.output, null, 2) }}</pre>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>
