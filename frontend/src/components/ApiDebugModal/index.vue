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
import { ref, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getApi, debugApi } from '@/api/apidoc'
import { getEnvironments } from '@/api/environment'
import CodeEditor from '@/components/CodeEditor/index.vue'
import { schemaToExampleString } from '@/utils/schemaToExample'
import { formatJson } from '@/utils/jsonFormat'

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
const queryParamValues = ref<Record<string, string>>({})
const pathParamValues = ref<Record<string, string>>({})
const headerValues = ref<Record<string, string>>({})
const debugResult = ref<any>(null)
const debugLoading = ref(false)

// 请求体编辑
const debugBodyType = ref('raw')
const debugRawType = ref('json')
const debugBody = ref('{}')

const methodColors: Record<string, string> = { GET: '', POST: 'success', PUT: 'warning', DELETE: 'danger', PATCH: 'info' }

// 调试弹窗顶部显示的完整 URL：host + 服务前缀 + path
const displayUrl = computed(() => {
  if (!apiInfo.value) return ''
  const path = apiInfo.value.path || ''
  const prefix = apiInfo.value.servicePrefix || ''
  const leadingMatch = path.match(/^\$\{[^}]*\}/)
  if (leadingMatch) {
    const host = leadingMatch[0]
    return host + prefix + path.slice(host.length)
  }
  return prefix + path
})

const bodyTypeOptions = [
  { value: 'none', label: 'none' },
  { value: 'form_data', label: 'form-data' },
  { value: 'x_www_form_urlencoded', label: 'x-www-form-urlencoded' },
  { value: 'raw', label: 'raw' },
  { value: 'binary', label: 'binary' },
  { value: 'graphql', label: 'GraphQL' },
]
const rawTypeOptions = [
  { value: 'text', label: 'Text' },
  { value: 'javascript', label: 'JavaScript' },
  { value: 'json', label: 'JSON' },
  { value: 'html', label: 'HTML' },
  { value: 'xml', label: 'XML' },
]

function parseArr(raw?: string): any[] {
  if (!raw) return []
  try { const a = JSON.parse(raw); return Array.isArray(a) ? a : [] } catch { return [] }
}

function isJsonSchema(obj: any): boolean {
  if (!obj || typeof obj !== 'object' || Array.isArray(obj)) return false
  const knownTypes = ['object', 'array', 'string', 'integer', 'number', 'boolean']
  if (knownTypes.includes(obj.type)) return true
  if (obj.properties && typeof obj.properties === 'object') return true
  return false
}

function stripJsonComments(text: string): string {
  let result = ''
  let i = 0
  while (i < text.length) {
    // 保留字符串字面量中的 //
    if (text[i] === '"' || text[i] === "'") {
      const quote = text[i]
      let j = i + 1
      while (j < text.length && text[j] !== quote) {
        if (text[j] === '\\') j++
        j++
      }
      result += text.substring(i, j + 1)
      i = j + 1
      continue
    }
    if (text[i] === '/' && text[i + 1] === '/') {
      while (i < text.length && text[i] !== '\n') i++
      continue
    }
    result += text[i]
    i++
  }
  return result
}

function defaultRequestBody(bodyType: string): string {
  if (bodyType === 'form_data' || bodyType === 'x_www_form_urlencoded' || bodyType === 'binary') {
    return '[]'
  }
  if (bodyType === 'graphql') {
    return JSON.stringify({ query: '', variables: '{}' }, null, 2)
  }
  if (bodyType === 'none') {
    return ''
  }
  return '{}'
}

function extractJsonBody(text: string): string {
  try {
    const obj = JSON.parse(text)
    return JSON.stringify(obj, null, 2)
  } catch {
    return '{}'
  }
}

function buildDebugPayloadBody(): string {
  if (debugBodyType.value === 'none') {
    return ''
  }
  if (debugBodyType.value === 'raw' && debugRawType.value === 'json') {
    return extractJsonBody(stripJsonComments(debugBody.value))
  }
  return debugBody.value
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
    const qv: Record<string, string> = {}
    qp.forEach((p: any) => { if (p.name) qv[p.name] = '' })
    queryParamValues.value = qv
    const pathWithoutPlaceholders = (res.data?.path || '').replace(/\$\{[^}]*\}/g, '')
    const pp = (pathWithoutPlaceholders.match(/\{(\w+)\}/g) || []).map((m: string) => m.slice(1, -1))
    const pv: Record<string, string> = {}
    pp.forEach((name: string) => { pv[name] = '' })
    pathParamValues.value = pv
    const hv: Record<string, string> = {}
    hp.forEach((p: any) => { if (p.name) hv[p.name] = p.value || '' })
    headerValues.value = hv
    // 初始化请求体：JSON Schema 自动转为示例值
    const bt = res.data?.bodyType || 'raw'
    debugBodyType.value = bt
    debugRawType.value = res.data?.rawType || 'json'
    const rawBody = res.data?.requestBody || ''
    let initialBody = rawBody || defaultRequestBody(bt)
    if (bt === 'raw' && debugRawType.value === 'json' && rawBody) {
      try {
        const parsed = JSON.parse(rawBody)
        if (isJsonSchema(parsed)) {
          initialBody = schemaToExampleString(parsed)
        }
      } catch { /* ignore */ }
    }
    debugBody.value = initialBody
    if (bt === 'raw' && debugRawType.value === 'json') {
      debugBody.value = formatJson(debugBody.value)
    }
    customParams.value = []
    debugResult.value = null
    respTabs.value = 'body'
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
  const pathWithoutPlaceholders = (info?.path || '').replace(/\$\{[^}]*\}/g, '')
  pathParams.value = (pathWithoutPlaceholders.match(/\{(\w+)\}/g) || []).map((m: string) => m.slice(1, -1))
})

// 弹窗打开时加载数据
watch(() => props.modelValue, (v) => {
  if (v && props.apiId) {
    loadApi()
    loadEnvs()
  }
})

// 请求体切换到 raw/JSON 时自动格式化
watch([debugBodyType, debugRawType], () => {
  if (debugBodyType.value === 'raw' && debugRawType.value === 'json') {
    debugBody.value = formatJson(debugBody.value)
  }
})

async function sendDebug() {
  if (!debugEnvId.value) {
    ElMessage.warning('请选择环境')
    return
  }
  debugLoading.value = true
  debugResult.value = null
  try {
    const payload: any = {
      environmentId: debugEnvId.value,
      pathParams: pathParamValues.value,
      queryParams: getAllQueryParamValues(),
      headers: headerValues.value,
    }
    if (apiInfo.value?.httpMethod !== 'GET' && debugBodyType.value !== 'none') {
      payload.body = buildDebugPayloadBody()
      payload.bodyType = debugBodyType.value
      payload.rawType = debugRawType.value
    }
    const res: any = await debugApi(props.projectId, props.apiId, payload)
    debugResult.value = res.data
  } catch (e: any) {
    debugResult.value = { success: 0, error: e?.response?.data?.message || e?.message || '调试失败' }
  } finally { debugLoading.value = false }
}

function formatResponseBody(result: any): string {
  const body = result?.responseBody ?? result?.output ?? result?.error ?? ''
  if (typeof body === 'string') return body
  return JSON.stringify(body, null, 2)
}

const headerEntries = computed(() => {
  const h = debugResult.value?.responseHeaders
  if (!h || typeof h !== 'object') return []
  return Object.entries(h).map(([key, value]) => ({ key, value: String(value) }))
})

const respTabs = ref('body')

// 自定义参数（手动添加）
const customParams = ref<{ name: string; type: string; value: string }[]>([])
function addCustomParam() {
  customParams.value.push({ name: '', type: 'string', value: '' })
}
function removeCustomParam(idx: number) {
  customParams.value.splice(idx, 1)
}

// 合并所有 Query 参数值（预定义 + 自定义）
function getAllQueryParamValues() {
  const merged: Record<string, string> = { ...queryParamValues.value }
  customParams.value.forEach((p) => {
    if (p.name) merged[p.name] = p.value
  })
  return merged
}

// 请求体键值对编辑（form-data / x-www-form-urlencoded / binary）
const debugBodyKvItems = computed({
  get: () => parseArr(debugBody.value),
  set: (val) => { debugBody.value = JSON.stringify(val) },
})

const debugGraphqlQuery = computed({
  get: () => {
    try { return JSON.parse(debugBody.value).query || '' } catch { return '' }
  },
  set: (val) => {
    try {
      const obj = JSON.parse(debugBody.value || '{}')
      obj.query = val
      debugBody.value = JSON.stringify(obj, null, 2)
    } catch {}
  },
})

const debugGraphqlVariables = computed({
  get: () => {
    try { return JSON.parse(debugBody.value).variables || '{}' } catch { return '{}' }
  },
  set: (val) => {
    try {
      const obj = JSON.parse(debugBody.value || '{}')
      obj.variables = val
      debugBody.value = JSON.stringify(obj, null, 2)
    } catch {}
  },
})

function addBodyRow() {
  const items = parseArr(debugBody.value)
  items.push({ name: '', type: 'string', required: false, description: '', value: '' })
  debugBody.value = JSON.stringify(items)
}
function removeBodyRow(idx: number) {
  const items = parseArr(debugBody.value)
  items.splice(idx, 1)
  debugBody.value = JSON.stringify(items)
}
</script>

<template>
  <el-dialog v-model="visible" title="在线调试" width="960px" top="5vh" @update:model-value="visible = $event">
    <div v-loading="loading" class="debug-modal-body">
      <!-- 接口信息 -->
      <div v-if="apiInfo" class="debug-info">
        <div class="debug-info-title">
          <span class="api-name">{{ apiInfo.name }}</span>
          <span v-if="apiInfo.description" class="api-desc">{{ apiInfo.description }}</span>
        </div>
        <div class="debug-info-url">
          <el-tag :type="methodColors[apiInfo.httpMethod] || 'info'" size="small">{{ apiInfo.httpMethod }}</el-tag>
          <code class="api-path">{{ displayUrl }}</code>
        </div>
      </div>

      <!-- 环境选择 -->
      <div class="debug-env-row">
        <span class="env-label">执行环境：</span>
        <el-select v-model="debugEnvId" placeholder="选择环境" style="width: 180px" size="small">
          <el-option v-for="env in environments" :key="env.id" :value="env.id" :label="env.name" />
        </el-select>
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
                  <el-input v-model="pathParamValues[row.name]" size="small" placeholder="值" />
                </template>
              </el-table-column>
            </el-table>
          </div>
          <div class="param-section">
            <div class="param-section-head">
              <span class="param-section-title">Query 参数</span>
              <el-button size="small" text type="primary" @click="addCustomParam" style="font-size: 12px;">+ 添加参数</el-button>
            </div>
            <el-table v-if="queryParams.length" :data="queryParams" size="small" border>
              <el-table-column prop="name" label="参数名" width="140">
                <template #default="{ row }">
                  <span style="font-family: monospace">{{ row.name }}</span>
                </template>
              </el-table-column>
              <el-table-column label="类型" width="80">
                <template #default="{ row }">
                  <el-tag size="small" type="info" style="font-family: monospace">{{ row.type || 'string' }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="值">
                <template #default="{ row }">
                  <el-input v-model="queryParamValues[row.name]" size="small" placeholder="输入值" />
                </template>
              </el-table-column>
            </el-table>
            <p v-else-if="!customParams.length" class="empty-hint">该接口无 Query 参数</p>
            <!-- 手动添加的自定义参数 -->
            <el-table v-if="customParams.length" :data="customParams" size="small" border style="margin-top: 4px">
              <el-table-column label="参数名" width="140">
                <template #default="{ row }">
                  <el-input v-model="row.name" size="small" placeholder="参数名" style="font-family: monospace" />
                </template>
              </el-table-column>
              <el-table-column label="类型" width="80">
                <template #default="{ row }">
                  <el-select v-model="row.type" size="small" style="width: 100%">
                    <el-option value="string" label="string" />
                    <el-option value="integer" label="integer" />
                    <el-option value="number" label="number" />
                    <el-option value="boolean" label="boolean" />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="值">
                <template #default="{ $index, row }">
                  <div style="display: flex; align-items: center; gap: 4px;">
                    <el-input v-model="row.value" size="small" placeholder="输入值" />
                    <el-button link size="small" type="danger" @click="removeCustomParam($index)">✕</el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </div>
          <div v-if="headerParams.length" class="param-section">
            <div class="param-section-title">Header</div>
            <el-table :data="headerParams" size="small" border>
              <el-table-column prop="name" label="参数名" width="160">
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
          <div v-if="apiInfo && apiInfo.httpMethod !== 'GET'" class="param-section">
            <div class="param-section-title">请求体</div>
            <div class="body-type-row">
              <el-radio-group v-model="debugBodyType" size="small">
                <el-radio-button v-for="b in bodyTypeOptions" :key="b.value" :value="b.value">{{ b.label }}</el-radio-button>
              </el-radio-group>
              <el-select v-if="debugBodyType === 'raw'" v-model="debugRawType" size="small" style="width: 130px">
                <el-option v-for="r in rawTypeOptions" :key="r.value" :value="r.value" :label="r.label" />
              </el-select>
              <el-button v-if="debugBodyType === 'raw' && debugRawType === 'json'" size="small" @click="debugBody = formatJson(debugBody)">格式化</el-button>
            </div>
            <div v-if="debugBodyType === 'none'" class="empty-hint">该接口无请求体</div>
            <div v-else-if="debugBodyType === 'raw'" style="height: 200px">
              <CodeEditor v-model="debugBody" :language="debugRawType === 'javascript' ? 'javascript' : debugRawType === 'json' ? 'json' : 'text'" :min-height="180" placeholder="请输入请求体" />
            </div>
            <div v-else-if="debugBodyType === 'graphql'">
              <div class="body-editor-row" style="height: 140px">
                <CodeEditor v-model="debugGraphqlQuery" language="text" :min-height="120" placeholder="请输入 GraphQL Query" />
              </div>
              <div class="body-editor-row" style="height: 100px; margin-top: 8px">
                <CodeEditor v-model="debugGraphqlVariables" language="json" :min-height="80" placeholder="请输入 Variables（JSON）" />
              </div>
            </div>
            <div v-else class="params-section">
              <div class="section-head">
                <span class="param-section-title">{{ debugBodyType === 'form_data' ? '表单参数' : debugBodyType === 'binary' ? '二进制参数' : '键值对参数' }}</span>
                <el-button size="small" @click="addBodyRow">+ 添加参数</el-button>
              </div>
              <el-table :data="debugBodyKvItems" size="small" border>
                <el-table-column label="参数名" width="140">
                  <template #default="{ row }"><el-input v-model="row.name" size="small" placeholder="参数名" /></template>
                </el-table-column>
                <el-table-column label="值">
                  <template #default="{ row }"><el-input v-model="row.value" size="small" placeholder="值" /></template>
                </el-table-column>
                <el-table-column label="操作" width="70">
                  <template #default="{ $index }">
                    <el-button link size="small" type="danger" @click="removeBodyRow($index)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
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
                {{ debugResult.statusCode || (debugResult.success === 1 ? '200 OK' : 'ERROR') }}
              </span>
              <span v-if="debugResult.responseTimeMs" class="resp-meta">耗时: {{ debugResult.responseTimeMs }}ms</span>
              <span v-if="debugResult.responseSize" class="resp-meta">大小: {{ debugResult.responseSize }}</span>
            </div>
            <div v-if="debugResult.requestUrl" class="resp-request-url">
              <span class="resp-meta">请求 URL：</span>
              <code class="url-code">{{ debugResult.requestUrl }}</code>
            </div>
            <el-tabs v-model="respTabs" class="resp-tabs">
              <el-tab-pane label="响应体" name="body">
                <pre class="resp-body-code">{{ formatResponseBody(debugResult) }}</pre>
              </el-tab-pane>
              <el-tab-pane label="响应头" name="headers">
                <el-table v-if="headerEntries.length" :data="headerEntries" size="small" border style="max-height: 300px; overflow: auto">
                  <el-table-column prop="key" label="Header" width="180">
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
                <p v-else class="empty-hint" style="padding: 20px; text-align: center">无响应头信息</p>
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
    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
      <el-button type="primary" :loading="debugLoading" @click="sendDebug">发送请求</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.debug-modal-body {
  min-height: 400px;
}
.debug-info {
  margin-bottom: 12px;
}
.debug-info-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}
.debug-info-url {
  display: flex;
  align-items: center;
  gap: 10px;
}
.api-name {
  font-size: 15px;
  font-weight: 600;
}
.api-desc {
  color: #909399;
  font-size: 13px;
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
  grid-template-columns: 1fr 0.75fr;
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
.param-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.param-section-title {
  font-size: 13px;
  font-weight: 600;
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
  font-size: 15px;
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
.resp-request-url {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 12px;
}
.url-code {
  font-family: monospace;
  color: #606266;
  word-break: break-all;
  line-height: 1.5;
}
.body-type-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}
.body-editor-row {
  border-radius: 0 0 6px 6px;
  overflow: hidden;
}
.params-section {
  margin-bottom: 16px;
}
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.resp-body-code {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
  max-height: 300px;
  overflow: auto;
  font-size: 12px;
  font-family: monospace;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
