<!--
 @author HXN
 @date 2026-08-21 15:30
 @description API 调试弹窗组件
-->
<script setup lang="ts">
/**
 * 接口在线调试弹窗
 * 双列布局：左侧请求参数输入，右侧响应结果
 * 对齐原型 api-list.html 的 debugModal
 */
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getApi, debugApi } from '@/api/apidoc'
import { getEnvironments } from '@/api/environment'

interface Props {
  modelValue: boolean
  projectId: number
  apiId: number
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
}>()

const visible = ref(props.modelValue)
watch(() => props.modelValue, (v) => { visible.value = v })
watch(visible, (v) => emit('update:modelValue', v))

const loading = ref(false)
const apiInfo = ref<any>(null)
const environments = ref<any[]>([])
const debugEnvId = ref<number | null>(null)
const paramValues = ref<Record<string, string>>({})
const headerValues = ref<Record<string, string>>({})
const debugResult = ref<any>(null)
const debugLoading = ref(false)

const methodColors: Record<string, string> = { GET: '', POST: 'success', PUT: 'warning', DELETE: 'danger', PATCH: 'info' }

function parseArr(raw?: string): any[] {
  if (!raw) return []
  try { const a = JSON.parse(raw); return Array.isArray(a) ? a : [] } catch { return [] }
}

async function loadApi() {
  if (!props.apiId) return
  loading.value = true
  debugResult.value = null
  try {
    const res: any = await getApi(props.projectId, props.apiId)
    apiInfo.value = res.data
    const qp = parseArr(res.data?.requestParams)
    const hp = parseArr(res.data?.headers)
    const pv: Record<string, string> = {}
    qp.forEach((p: any) => { if (p.name) pv[p.name] = '' })
    paramValues.value = pv
    const hv: Record<string, string> = {}
    hp.forEach((p: any) => { if (p.name) hv[p.name] = '' })
    headerValues.value = hv
  } catch { ElMessage.error('加载接口失败') } finally { loading.value = false }
}

async function loadEnvs() {
  try {
    const res: any = await getEnvironments(props.projectId)
    environments.value = res.data || []
  } catch { environments.value = [] }
}

const queryParams = ref<any[]>([])
const headerParams = ref<any[]>([])
const pathParams = ref<string[]>([])
watch(apiInfo, (info) => {
  queryParams.value = parseArr(info?.requestParams)
  headerParams.value = parseArr(info?.headers)
  pathParams.value = (info?.path?.match(/\{(\w+)\}/g) || []).map((m: string) => m.slice(1, -1))
})

// 弹窗打开时加载数据
watch(() => props.modelValue, (v) => {
  if (v && props.apiId) {
    loadApi()
    loadEnvs()
  }
})

async function sendDebug() {
  debugLoading.value = true
  debugResult.value = null
  try {
    const res: any = await debugApi(props.projectId, props.apiId, {
      environmentId: debugEnvId.value || undefined,
      params: paramValues.value,
      headers: headerValues.value,
    })
    debugResult.value = res.data
  } catch (e: any) {
    debugResult.value = { success: 0, error: e?.response?.data?.message || e?.message || '调试失败' }
  } finally { debugLoading.value = false }
}

const respTabs = ref('body')
</script>

<template>
  <el-dialog v-model="visible" title="在线调试" width="960px" top="5vh" @update:model-value="visible = $event">
    <div v-loading="loading" class="debug-modal-body">
      <!-- 接口信息 -->
      <div v-if="apiInfo" class="debug-info">
        <span class="api-name">{{ apiInfo.name }}</span>
        <el-tag :type="methodColors[apiInfo.httpMethod] || 'info'" size="small">{{ apiInfo.httpMethod }}</el-tag>
        <code class="api-path">{{ apiInfo.path }}</code>
      </div>

      <!-- 环境选择 -->
      <div class="debug-env-row">
        <span class="env-label">执行环境：</span>
        <el-select v-model="debugEnvId" placeholder="选择环境" style="width: 180px" size="small">
          <el-option v-for="env in environments" :key="env.id" :value="env.id" :label="env.name" />
        </el-select>
        <el-button type="primary" size="small" :loading="debugLoading" @click="sendDebug">发送请求</el-button>
      </div>

      <!-- 双列：左请求 / 右响应 -->
      <div class="debug-grid">
        <div class="debug-col">
          <div class="debug-col-title">请求参数</div>
          <div v-if="pathParams.length" class="param-section">
            <div class="param-section-title">Path 参数</div>
            <el-table :data="pathParams.map((p) => ({ name: p }))" size="small" border>
              <el-table-column prop="name" label="参数名" />
              <el-table-column label="值">
                <template #default="{ row }">
                  <el-input v-model="paramValues[row.name]" size="small" placeholder="值" />
                </template>
              </el-table-column>
            </el-table>
          </div>
          <div class="param-section">
            <div class="param-section-title">Query 参数</div>
            <el-table v-if="queryParams.length" :data="queryParams" size="small" border>
              <el-table-column prop="name" label="参数名" width="140" />
              <el-table-column label="值">
                <template #default="{ row }">
                  <el-input v-model="paramValues[row.name]" size="small" placeholder="值" />
                </template>
              </el-table-column>
            </el-table>
            <p v-else class="empty-hint">无 Query 参数</p>
          </div>
          <div v-if="headerParams.length" class="param-section">
            <div class="param-section-title">Header</div>
            <el-table :data="headerParams" size="small" border>
              <el-table-column prop="name" label="参数名" width="160" />
              <el-table-column label="值">
                <template #default="{ row }">
                  <el-input v-model="headerValues[row.name]" size="small" placeholder="值" />
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>

        <div class="debug-col">
          <div class="debug-col-title">响应结果</div>
          <div v-if="!debugResult" class="response-empty">
            <div style="font-size: 36px; opacity: 0.3">📡</div>
            <div>填写参数后点击「发送请求」</div>
          </div>
          <div v-else class="response-result">
            <div class="resp-status-bar">
              <span :class="['resp-code', debugResult.success === 1 ? 'ok' : 'err']">
                {{ debugResult.success === 1 ? '成功' : '失败' }}
              </span>
              <span v-if="debugResult.responseTimeMs" class="resp-meta">耗时: {{ debugResult.responseTimeMs }}ms</span>
              <span v-if="debugResult.responseSize" class="resp-meta">大小: {{ debugResult.responseSize }}</span>
            </div>
            <el-tabs v-model="respTabs" class="resp-tabs">
              <el-tab-pane label="响应体" name="body">
                <el-input
                  :model-value="JSON.stringify(debugResult.responseBody ?? debugResult.output ?? debugResult.error ?? '', null, 2)"
                  type="textarea" :rows="12" readonly style="font-family: monospace; font-size: 12px"
                />
              </el-tab-pane>
              <el-tab-pane label="状态信息" name="status">
                <el-descriptions :column="1" border size="small">
                  <el-descriptions-item label="状态码">{{ debugResult.statusCode ?? (debugResult.success === 1 ? '200' : '-') }}</el-descriptions-item>
                  <el-descriptions-item label="响应时间">{{ debugResult.responseTimeMs ?? '-' }} ms</el-descriptions-item>
                  <el-descriptions-item label="错误信息">{{ debugResult.error || '-' }}</el-descriptions-item>
                </el-descriptions>
              </el-tab-pane>
            </el-tabs>
          </div>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<style scoped>
.debug-modal-body {
  min-height: 400px;
}
.debug-info {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}
.api-name {
  font-size: 15px;
  font-weight: 600;
}
.api-path {
  font-size: 13px;
  color: #606266;
  font-family: monospace;
}
.debug-env-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.env-label {
  font-size: 13px;
  color: #606266;
  white-space: nowrap;
}
.debug-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  min-height: 360px;
}
.debug-col {
  padding: 16px;
  overflow-y: auto;
  max-height: 56vh;
}
.debug-col:first-child {
  border-right: 1px solid #ebeef5;
}
.debug-col-title {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 12px;
}
.param-section {
  margin-bottom: 16px;
}
.param-section-title {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 8px;
  color: #606266;
}
.empty-hint {
  color: #909399;
  font-size: 12px;
  margin: 0;
}
.response-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 240px;
  color: #909399;
  gap: 8px;
}
.resp-status-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 8px;
}
.resp-code {
  font-weight: 700;
}
.resp-code.ok {
  color: #67c23a;
}
.resp-code.err {
  color: #f56c6c;
}
.resp-meta {
  font-size: 12px;
  color: #909399;
}
</style>
