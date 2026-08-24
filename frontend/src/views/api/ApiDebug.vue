<!--
 @author HXN
 @date 2026-08-20 15:34
 @description API 调试视图
-->
<script setup lang="ts">
/**
 * 接口在线调试 - M4
 * 双栏布局：左侧请求参数 / 右侧响应结果
 * 对齐原型 api-debug.html
 */
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getApi, debugApi } from '@/api/apidoc'
import { getEnvironments } from '@/api/environment'

const route = useRoute()
const router = useRouter()
const projectId = computed(() => Number(route.params.id))
const apiId = computed(() => Number(route.params.apiId))

const apiInfo = ref<any>({})
const environments = ref<any[]>([])
const debugResult = ref<any>(null)
const debugLoading = ref(false)
const envId = ref<number | null>(null)
const respTab = ref('body')

const methodColors: Record<string, string> = { GET: '', POST: 'success', PUT: 'warning', DELETE: 'danger', PATCH: 'info' }

// 参数解析
function parseArr(raw?: string): any[] {
  if (!raw) return []
  try { const a = JSON.parse(raw); return Array.isArray(a) ? a : [] } catch { return [] }
}
const queryParams = ref<any[]>([])
const headerParams = ref<any[]>([])
const paramValues = ref<Record<string, string>>({})
const headerValues = ref<Record<string, string>>({})

async function fetchApi() {
  try {
    const res: any = await getApi(projectId.value, apiId.value)
    apiInfo.value = res.data || {}
    queryParams.value = parseArr(res.data?.requestParams)
    headerParams.value = parseArr(res.data?.headers)
    const pv: Record<string, string> = {}
    queryParams.value.forEach((p: any) => { if (p.name) pv[p.name] = '' })
    paramValues.value = pv
    const hv: Record<string, string> = {}
    headerParams.value.forEach((p: any) => { if (p.name) hv[p.name] = p.value || '' })
    headerValues.value = hv
  } catch { ElMessage.error('加载接口信息失败') }
}

async function fetchEnvironments() {
  try {
    const res: any = await getEnvironments(projectId.value)
    environments.value = res.data || []
  } catch { /* ignore */ }
}

async function sendRequest() {
  debugLoading.value = true
  debugResult.value = null
  try {
    const res: any = await debugApi(projectId.value, apiId.value, {
      environmentId: envId.value || undefined,
      params: paramValues.value,
      headers: headerValues.value,
    })
    debugResult.value = res.data
  } catch (e: any) {
    debugResult.value = { success: 0, errorMessage: e?.message || '请求失败' }
  } finally { debugLoading.value = false }
}

function formatBody(result: any): string {
  const body = result?.responseBody ?? result?.output ?? result?.error ?? ''
  if (typeof body === 'string') return body
  return JSON.stringify(body, null, 2)
}

const headerEntries = computed(() => {
  const h = debugResult.value?.responseHeaders
  if (!h || typeof h !== 'object') return []
  return Object.entries(h).map(([key, value]) => ({ key, value: String(value) }))
})

onMounted(() => { fetchApi(); fetchEnvironments() })
</script>

<template>
  <div>
    <!-- 页头 -->
    <div class="debug-page-header">
      <div class="debug-header-left">
        <el-button type="primary" link @click="router.back()">← 返回</el-button>
        <el-tag :type="methodColors[apiInfo.httpMethod] || 'info'" size="small" style="font-size: 13px; height: 24px; min-width: 56px; text-align: center">
          {{ apiInfo.httpMethod }}
        </el-tag>
        <h2 style="margin: 0; font-size: 16px">{{ apiInfo.name }}</h2>
        <code class="header-path">{{ apiInfo.path }}</code>
      </div>
      <el-button type="primary" :loading="debugLoading" @click="sendRequest">发送请求</el-button>
    </div>

    <!-- 双栏布局 -->
    <div class="debug-layout">
      <!-- 左栏：请求参数 -->
      <div class="debug-panel">
        <div class="panel-header"><h3>请求参数</h3></div>
        <div class="panel-body">
          <div v-if="queryParams.length" class="param-group">
            <div class="param-group-title">Query 参数</div>
            <el-table :data="queryParams" size="small" border>
              <el-table-column label="参数名" width="180">
                <template #default="{ row }">
                  <span style="font-family: monospace">{{ row.name }}</span>
                </template>
              </el-table-column>
              <el-table-column label="值">
                <template #default="{ row }">
                  <el-input v-model="paramValues[row.name]" size="small" placeholder="值" />
                </template>
              </el-table-column>
            </el-table>
          </div>
          <div v-if="headerParams.length" class="param-group">
            <div class="param-group-title">Header 参数</div>
            <el-table :data="headerParams" size="small" border>
              <el-table-column label="参数名" width="200">
                <template #default="{ row }">
                  <span style="font-family: monospace">{{ row.name }}</span>
                </template>
              </el-table-column>
              <el-table-column label="值">
                <template #default="{ row }">
                  <el-input v-model="headerValues[row.name]" size="small" placeholder="值" />
                </template>
              </el-table-column>
            </el-table>
          </div>
          <div v-if="!queryParams.length && !headerParams.length" style="color: #909399; text-align: center; padding: 40px">
            该接口无请求参数
          </div>
        </div>
      </div>

      <!-- 右栏：响应结果 -->
      <div class="debug-panel">
        <div class="panel-header"><h3>响应结果</h3></div>
        <div class="panel-body">
          <!-- 空状态 -->
          <div v-if="!debugResult" class="response-empty">
            <div style="font-size: 36px; opacity: 0.3">📡</div>
            <div>填写参数后点击「发送请求」</div>
          </div>
          <!-- 响应内容 -->
          <div v-else>
            <div class="status-bar">
              <span :class="['status-code', debugResult.success === 1 ? 'ok' : 'err']">
                {{ debugResult.statusCode || (debugResult.success === 1 ? '200 OK' : 'ERROR') }}
              </span>
              <span v-if="debugResult.responseTimeMs" class="meta-info">耗时: {{ debugResult.responseTimeMs }}ms</span>
              <span v-if="debugResult.responseSize" class="meta-info">大小: {{ debugResult.responseSize }}</span>
            </div>
            <div v-if="debugResult.errorMessage" style="color: #f56c6c; margin-bottom: 8px; font-size: 13px">{{ debugResult.errorMessage }}</div>
            <el-tabs v-model="respTab">
              <el-tab-pane label="响应体" name="body">
                <pre class="response-body">{{ formatBody(debugResult) }}</pre>
              </el-tab-pane>
              <el-tab-pane label="响应头" name="headers">
                <el-table v-if="headerEntries.length" :data="headerEntries" size="small" border>
                  <el-table-column prop="key" label="Header" width="200">
                    <template #default="{ row }">
                      <span style="font-family: monospace; font-weight: 500">{{ row.key }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column prop="value" label="Value">
                    <template #default="{ row }">
                      <span style="font-family: monospace; word-break: break-all">{{ row.value }}</span>
                    </template>
                  </el-table-column>
                </el-table>
                <p v-else style="color: #909399; text-align: center; padding: 20px">无响应头信息</p>
              </el-tab-pane>
              <el-tab-pane label="状态信息" name="status">
                <el-descriptions :column="1" border size="small">
                  <el-descriptions-item label="状态码">{{ debugResult.statusCode ?? (debugResult.success === 1 ? '200 OK' : '-') }}</el-descriptions-item>
                  <el-descriptions-item label="响应时间">{{ debugResult.responseTimeMs ?? '-' }} ms</el-descriptions-item>
                  <el-descriptions-item label="响应大小">{{ debugResult.responseSize ?? '-' }}</el-descriptions-item>
                  <el-descriptions-item label="协议">{{ debugResult.protocol ?? 'HTTP/1.1' }}</el-descriptions-item>
                </el-descriptions>
              </el-tab-pane>
            </el-tabs>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.debug-page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.debug-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.header-path {
  color: #606266;
  font-size: 13px;
  font-family: monospace;
}
.debug-layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  min-height: calc(100vh - 200px);
}
.debug-panel {
  background: #fff;
  border-radius: 6px;
  border: 1px solid #ebeef5;
  overflow: hidden;
}
.panel-header {
  padding: 12px 16px;
  border-bottom: 1px solid #ebeef5;
  background: #fafafa;
}
.panel-header h3 {
  font-size: 14px;
  font-weight: 600;
  margin: 0;
}
.panel-body {
  padding: 16px;
}
.param-group {
  margin-bottom: 16px;
}
.param-group-title {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 8px;
  color: #606266;
}
.response-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 300px;
  color: #909399;
  gap: 8px;
}
.status-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
}
.status-code {
  font-weight: 700;
  font-size: 15px;
}
.status-code.ok {
  color: #67c23a;
}
.status-code.err {
  color: #f56c6c;
}
.meta-info {
  font-size: 12px;
  color: #909399;
}
.response-body {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
  max-height: 400px;
  overflow: auto;
  font-size: 12px;
  font-family: monospace;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
