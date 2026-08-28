<!--
 @author HXN
 @date 2026-08-20 15:34
 @description API 编辑视图
-->
<script setup lang="ts">
/**
 * 接口编辑/新建 - M4
 * 7 Tab：基础信息 / Header 参数 / 请求参数 / 请求体 / 响应体 / 调试 / 引用关系
 * 对齐原型 api-edit.html（请求参数/响应改为可视化表格编辑，数据仍序列化为 JSON 存储）
 */
defineOptions({ name: 'ApiEdit' })
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getApi, createApi, updateApi, getModules, getApiReferences, debugApi } from '@/api/apidoc'
import { getEnvironments } from '@/api/environment'
import { useDict } from '@/composables/useDict'
import { usePermission } from '@/composables/usePermission'
import EditPageHeader from '@/components/EditPageHeader/index.vue'
import CodeEditor from '@/components/CodeEditor/index.vue'
import { schemaToExampleString } from '@/utils/schemaToExample'
import { formatJson } from '@/utils/jsonFormat'

const route = useRoute()
const router = useRouter()
const projectId = computed(() => Number(route.params.id))
const apiId = computed(() => Number(route.params.apiId))
const isEdit = computed(() => !!apiId.value)
const { hasPermission } = usePermission()

const activeTab = ref('basic')
const loading = ref(false)
const modules = ref<any[]>([])
const environments = ref<any[]>([])
const { options: httpMethodOptions } = useDict('http_method')
const { options: paramTypeOptions } = useDict('param_type')

const form = reactive({
  name: '', httpMethod: 'GET', path: '', host: '', service: '', moduleId: null as number | null,
  description: '', requestParams: '[]', requestBody: '{}', responseBody: '{}', headers: '[]',
  contentType: 'application/json', bodyType: 'raw', rawType: 'json',
  sourceType: 'MANUAL',
})

// 请求体格式选项
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

function mapBodyTypeToContentType(bodyType: string, rawType: string): string {
  if (bodyType === 'form_data') return 'multipart/form-data'
  if (bodyType === 'x_www_form_urlencoded') return 'application/x-www-form-urlencoded'
  if (bodyType === 'binary') return 'application/octet-stream'
  if (bodyType === 'graphql') return 'application/json'
  if (rawType === 'text') return 'text/plain'
  if (rawType === 'javascript') return 'application/javascript'
  if (rawType === 'json') return 'application/json'
  if (rawType === 'html') return 'text/html'
  if (rawType === 'xml') return 'application/xml'
  return 'application/json'
}

const bodyKvItems = computed({
  get: () => parseArr(form.requestBody),
  set: (val) => { form.requestBody = JSON.stringify(val) },
})

const graphqlQuery = computed({
  get: () => {
    try { return JSON.parse(form.requestBody).query || '' } catch { return '' }
  },
  set: (val) => {
    try {
      const obj = JSON.parse(form.requestBody || '{}')
      obj.query = val
      form.requestBody = JSON.stringify(obj, null, 2)
    } catch {}
  },
})

const graphqlVariables = computed({
  get: () => {
    try { return JSON.parse(form.requestBody).variables || '{}' } catch { return '{}' }
  },
  set: (val) => {
    try {
      const obj = JSON.parse(form.requestBody || '{}')
      obj.variables = val
      form.requestBody = JSON.stringify(obj, null, 2)
    } catch {}
  },
})

// ===== 参数可视化编辑（序列化为 JSON 存入 form） =====
function parseArr(raw?: string): any[] {
  if (!raw) return []
  try { const a = JSON.parse(raw); return Array.isArray(a) ? a : [] } catch { return [] }
}

function looksLikeSchema(text: string): boolean {
  try {
    const obj = JSON.parse(text)
    if (!obj || typeof obj !== 'object') return false
    // 对象 schema：有 type + properties
    if ('type' in obj && 'properties' in obj) return true
    // 数组 schema：有 type=array + items
    if (obj.type === 'array' && obj.items) return true
    return false
  } catch {
    return false
  }
}

function stripJsonComments(text: string): string {
  return text
    .replace(/\/\*[\s\S]*?\*\//g, '')
    .replace(/\/\/.*$/gm, '')
    .replace(/,\s*([}\]])/g, '$1')
}

function extractJsonBody(text: string): string {
  try {
    const cleaned = stripJsonComments(text)
    const obj = JSON.parse(cleaned)
    return JSON.stringify(obj, null, 2)
  } catch {
    return '{}'
  }
}

const queryParams = ref<any[]>([])
const headerParams = ref<any[]>([])

function syncToForm() {
  form.requestParams = JSON.stringify(queryParams.value)
  form.headers = JSON.stringify(headerParams.value)
  // Content-Type 统一在 Header 参数 tab 管理，同步回 form.contentType 保持后端兼容
  const ct = headerParams.value.find((h: any) => h.name && h.name.toLowerCase() === 'content-type')
  form.contentType = ct ? (ct.value || '') : ''
}

watch([queryParams, headerParams], syncToForm, { deep: true })

// bodyType/rawType 变化时同步 Content-Type 请求头
watch([() => form.bodyType, () => form.rawType], () => {
  if (form.httpMethod === 'GET') return
  const ct = mapBodyTypeToContentType(form.bodyType, form.rawType)
  form.contentType = ct
  const idx = headerParams.value.findIndex((h: any) => h.name && h.name.toLowerCase() === 'content-type')
  if (idx >= 0) {
    headerParams.value[idx].value = ct
  } else {
    headerParams.value.unshift({ name: 'Content-Type', type: 'string', required: false, description: '内容类型', value: ct })
  }
})

function addRow(arr: any[]) {
  arr.push({ name: '', type: 'string', required: false, description: '', value: '' })
}
function removeRow(arr: any[], idx: number) {
  arr.splice(idx, 1)
}

// Path 参数从路径解析（只读展示，排除 ${...} 环境变量占位符）
const pathParams = computed(() => {
  const matches = form.path.replace(/\$\{[^}]*\}/g, '').match(/\{(\w+)\}/g) || []
  return matches.map((m) => m.slice(1, -1))
})

// ===== 数据加载 =====
async function fetchModules() {
  try {
    const res: any = await getModules(projectId.value)
    // 排除"全部"系统分组（接口不应直接归属"全部"），保留"未分类"及其他用户分组
    modules.value = (res.data || []).filter((m: any) => !(m.isSystem === 1 && m.name === '全部'))
    if (!form.moduleId && modules.value.length) form.moduleId = modules.value[0].id
  } catch { modules.value = [] }
}

async function fetchEnvironments() {
  try {
    const res: any = await getEnvironments(projectId.value)
    environments.value = res.data || []
  } catch { environments.value = [] }
}

async function fetchApi() {
  if (!isEdit.value) return
  loading.value = true
  try {
    const res: any = await getApi(projectId.value, apiId.value)
    Object.assign(form, res.data)
    // 将 path 中的前导 ${host} 占位符拆分到独立 host 输入框
    const leading = form.path.match(/^\$\{[^}]*\}/)
    if (leading) {
      form.host = leading[0]
      form.path = form.path.slice(leading[0].length)
    } else {
      form.host = ''
    }
    if (!form.bodyType) form.bodyType = 'raw'
    if (!form.rawType) form.rawType = 'json'
    if (!form.requestBody) form.requestBody = defaultRequestBody(form.bodyType)
    if (looksLikeSchema(form.requestBody)) {
      form.requestBody = schemaToExampleString(JSON.parse(form.requestBody))
      form.bodyType = 'raw'
      form.rawType = 'json'
    } else {
      // 解包后端 sanitizeJson 包装的纯文本（"plain text" → plain text）
      try {
        const parsed = JSON.parse(form.requestBody)
        if (typeof parsed === 'string') form.requestBody = parsed
      } catch { /* already valid object/array or not JSON */ }
    }
    if (!form.responseBody) form.responseBody = '{}'
    if (looksLikeSchema(form.responseBody)) {
      form.responseBody = schemaToExampleString(JSON.parse(form.responseBody))
    } else {
      try {
        const parsed = JSON.parse(form.responseBody)
        if (typeof parsed === 'string') form.responseBody = parsed
      } catch { /* ignore */ }
    }
    queryParams.value = parseArr(form.requestParams)
    headerParams.value = parseArr(form.headers)
    // 将 contentType 合并到 headerParams（统一在 Header 参数 tab 管理）
    if (form.contentType) {
      const hasCT = headerParams.value.some((h: any) => h.name && h.name.toLowerCase() === 'content-type')
      if (!hasCT) {
        headerParams.value.unshift({ name: 'Content-Type', type: 'string', required: false, description: '内容类型', value: form.contentType })
      }
    }
  } catch { ElMessage.error('加载接口失败') } finally { loading.value = false }
}

// ===== 引用关系 =====
const references = ref<any[]>([])
const refsLoading = ref(false)
async function fetchReferences() {
  if (!isEdit.value) return
  refsLoading.value = true
  try {
    const res: any = await getApiReferences(projectId.value, apiId.value)
    references.value = res.data || []
  } catch { references.value = [] } finally { refsLoading.value = false }
}

// ===== 调试 =====
const debugEnvId = ref<number | null>(null)
const debugParamValues = ref<Record<string, string>>({})
const debugBody = ref('{}')
const debugBodyType = ref('raw')
const debugRawType = ref('json')
const debugResult = ref<any>(null)
const debugLoading = ref(false)

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

function buildDebugPayloadBody(): string {
  if (debugBodyType.value === 'none') {
    return ''
  }
  if (debugBodyType.value === 'raw' && debugRawType.value === 'json') {
    return extractJsonBody(debugBody.value)
  }
  return debugBody.value
}

function initDebugParams() {
  const map: Record<string, string> = {}
  queryParams.value.forEach((p) => { map[p.name] = '' })
  debugParamValues.value = map
  debugBodyType.value = form.bodyType || 'raw'
  debugRawType.value = form.rawType || 'json'
  debugBody.value = form.requestBody || defaultRequestBody(debugBodyType.value)
}
async function sendDebug() {
  if (!debugEnvId.value) {
    ElMessage.warning('请选择环境')
    return
  }
  debugLoading.value = true
  debugResult.value = null
  try {
    const payload: any = {
      environmentId: debugEnvId.value || undefined,
      queryParams: debugParamValues.value,
    }
    if (form.httpMethod !== 'GET' && debugBodyType.value !== 'none') {
      payload.body = buildDebugPayloadBody()
      payload.bodyType = debugBodyType.value
      payload.rawType = debugRawType.value
    }
    const res: any = await debugApi(projectId.value, apiId.value, payload)
    debugResult.value = res.data
  } catch (e: any) {
    debugResult.value = { success: 0, error: e?.response?.data?.message || e?.message || '调试失败' }
  } finally { debugLoading.value = false }
}

function onTabChange(tab: string) {
  if (tab === 'debug') initDebugParams()
  if (tab === 'refs') fetchReferences()
}

// ===== 调试响应辅助 =====
const debugRespTab = ref('body')
const debugHeaderEntries = computed(() => {
  const h = debugResult.value?.responseHeaders
  if (!h || typeof h !== 'object') return []
  return Object.entries(h).map(([key, value]) => ({ key, value: String(value) }))
})
function formatDebugBody(result: any): string {
  const body = result?.responseBody ?? result?.output ?? result?.error ?? ''
  if (typeof body === 'string') return body
  return JSON.stringify(body, null, 2)
}

// ===== 保存 =====
async function handleSubmit() {
  syncToForm()
  if (!form.name || !form.path) { ElMessage.warning('请填写必填项'); activeTab.value = 'basic'; return }
  try {
    const payload = { ...form, path: (form.host || '') + form.path, projectId: projectId.value }
    delete (payload as any).host
    if (isEdit.value) {
      await updateApi(projectId.value, apiId.value, payload)
      ElMessage.success('更新成功')
    } else {
      await createApi(projectId.value, payload)
      ElMessage.success('创建成功')
    }
    router.push(`/project/${projectId.value}/apis`)
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '操作失败') }
}

onMounted(() => {
  fetchModules()
  fetchEnvironments()
  fetchApi()
  // 新建时自动添加 Content-Type 到 Header 参数
  if (!isEdit.value) {
    headerParams.value.push({ name: 'Content-Type', type: 'string', required: false, description: '内容类型', value: form.contentType })
  }
})
</script>

<template>
  <div v-loading="loading">
    <EditPageHeader :title="isEdit ? '编辑接口' : '新建接口'">
        <el-button v-if="hasPermission('project:api:edit')" type="primary" @click="handleSubmit">保存</el-button>
        <el-button @click="router.back()">取消</el-button>
    </EditPageHeader>

    <el-tabs v-model="activeTab" @tab-change="(t: string) => onTabChange(t)">
      <!-- Tab: 基础信息 -->
      <el-tab-pane label="基础信息" name="basic">
        <el-card shadow="never">
          <el-form label-position="top">
            <el-row :gutter="16" v-if="isEdit">
              <el-col :span="12">
                <el-form-item label="接口 ID">
                  <el-input :model-value="apiId" disabled />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="来源">
                  <el-input :model-value="form.sourceType === 'SWAGGER_IMPORT' ? 'Swagger 导入' : '手动创建'" disabled />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="接口名称" required>
                  <el-input v-model="form.name" placeholder="请输入接口名称" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="HTTP 方法">
                  <el-select v-model="form.httpMethod" style="width: 100%">
                    <el-option v-for="m in httpMethodOptions" :key="m.value" :value="m.value" :label="m.label" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="16">
              <el-col :span="6">
                <el-form-item label="Host">
                  <el-input v-model="form.host" placeholder="${host}" style="font-family: monospace" />
                </el-form-item>
              </el-col>
              <el-col :span="18">
                <el-form-item label="接口路径" required>
                  <el-input v-model="form.path" placeholder="/api/v1/example" style="font-family: monospace" />
                  <div class="path-hint">Host 以 ${host} 等占位符开头时，调试/执行将用所选环境的对应变量值作为请求地址</div>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="所属分组">
                  <el-select v-model="form.moduleId" placeholder="选择分组" style="width: 100%">
                    <el-option v-for="m in modules" :key="m.id" :value="m.id" :label="m.name" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="描述">
              <el-input v-model="form.description" type="textarea" :rows="2" placeholder="接口描述（可选）" />
            </el-form-item>
          </el-form>
        </el-card>
      </el-tab-pane>

      <!-- Tab: Header 参数 -->
      <el-tab-pane label="Header 参数" name="headers">
        <div class="params-section">
          <div class="section-head">
            <h4>Header 参数</h4>
            <el-button size="small" @click="addRow(headerParams)">+ 添加参数</el-button>
          </div>
          <el-table :data="headerParams" size="small" border>
            <el-table-column label="参数名" width="180">
              <template #default="{ row }"><el-input v-model="row.name" size="small" placeholder="如 Authorization" /></template>
            </el-table-column>
            <el-table-column label="类型" width="120">
              <template #default="{ row }">
                <el-select v-model="row.type" size="small">
                  <el-option v-for="t in paramTypeOptions" :key="t.value" :value="t.value" :label="t.label" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="必填" width="70">
              <template #default="{ row }"><el-switch v-model="row.required" /></template>
            </el-table-column>
            <el-table-column label="值" min-width="300">
              <template #default="{ row }"><el-input v-model="row.value" size="small" placeholder="如 application/json" /></template>
            </el-table-column>
            <el-table-column label="说明" width="160">
              <template #default="{ row }"><el-input v-model="row.description" size="small" /></template>
            </el-table-column>
            <el-table-column label="操作" width="70">
              <template #default="{ $index }">
                <el-button link size="small" type="danger" @click="removeRow(headerParams, $index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- Tab: 请求参数 -->
      <el-tab-pane label="请求参数" name="params">
        <div class="params-section">
          <h4>Path 参数</h4>
          <p v-if="!pathParams.length" class="empty-hint">该接口无 Path 参数</p>
          <el-table v-else :data="pathParams.map(p => ({ name: p }))" size="small" border>
            <el-table-column prop="name" label="参数名" />
            <el-table-column label="类型"><template #default>string</template></el-table-column>
            <el-table-column label="说明"><template #default>路径参数</template></el-table-column>
          </el-table>
        </div>
        <div class="params-section">
          <div class="section-head">
            <h4>Query 参数</h4>
            <el-button size="small" @click="addRow(queryParams)">+ 添加参数</el-button>
          </div>
          <el-table :data="queryParams" size="small" border>
            <el-table-column label="参数名" width="160">
              <template #default="{ row }"><el-input v-model="row.name" size="small" placeholder="参数名" /></template>
            </el-table-column>
            <el-table-column label="类型" width="120">
              <template #default="{ row }">
                <el-select v-model="row.type" size="small">
                  <el-option v-for="t in paramTypeOptions" :key="t.value" :value="t.value" :label="t.label" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="必填" width="70">
              <template #default="{ row }"><el-switch v-model="row.required" /></template>
            </el-table-column>
            <el-table-column label="说明">
              <template #default="{ row }"><el-input v-model="row.description" size="small" placeholder="说明" /></template>
            </el-table-column>
            <el-table-column label="操作" width="70">
              <template #default="{ $index }">
                <el-button link size="small" type="danger" @click="removeRow(queryParams, $index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- Tab: 请求体 -->
      <el-tab-pane label="请求体" name="body">
        <div v-if="form.httpMethod === 'GET'" class="empty-state">
          <div class="empty-icon">📋</div>
          <div>GET 请求不包含请求体</div>
        </div>
        <template v-else>
          <div class="body-type-row">
            <el-radio-group v-model="form.bodyType" size="small">
              <el-radio-button v-for="b in bodyTypeOptions" :key="b.value" :value="b.value">{{ b.label }}</el-radio-button>
            </el-radio-group>
            <el-select v-if="form.bodyType === 'raw'" v-model="form.rawType" size="small" style="width: 130px">
              <el-option v-for="r in rawTypeOptions" :key="r.value" :value="r.value" :label="r.label" />
            </el-select>
            <el-button v-if="form.bodyType === 'raw' && form.rawType === 'json'" size="small" @click="form.requestBody = formatJson(form.requestBody)">格式化</el-button>
          </div>
          <div v-if="form.bodyType === 'none'" class="empty-state" style="padding: 32px">
            <div>该接口无请求体</div>
          </div>
          <div v-else-if="form.bodyType === 'raw'" style="height: 360px">
            <CodeEditor v-model="form.requestBody" :language="form.rawType === 'javascript' ? 'javascript' : form.rawType === 'json' ? 'json' : 'text'" :min-height="320" placeholder="请输入请求体..." />
          </div>
          <div v-else-if="form.bodyType === 'graphql'">
            <div class="body-editor-row" style="height: 240px">
              <CodeEditor v-model="graphqlQuery" language="text" :min-height="200" placeholder="请输入 GraphQL Query" />
            </div>
            <div class="body-editor-row" style="height: 160px; margin-top: 12px">
              <CodeEditor v-model="graphqlVariables" language="json" :min-height="120" placeholder="请输入 Variables（JSON）" />
            </div>
          </div>
          <div v-else class="params-section">
            <div class="section-head">
              <h4>{{ form.bodyType === 'form_data' ? '表单参数' : form.bodyType === 'binary' ? '二进制参数' : '表单参数' }}</h4>
              <el-button size="small" @click="addRow(bodyKvItems)">+ 添加参数</el-button>
            </div>
            <el-table :data="bodyKvItems" size="small" border>
              <el-table-column label="参数名" width="200">
                <template #default="{ row }"><el-input v-model="row.name" size="small" placeholder="参数名" /></template>
              </el-table-column>
              <el-table-column label="值">
                <template #default="{ row }"><el-input v-model="row.value" size="small" placeholder="值" /></template>
              </el-table-column>
              <el-table-column label="操作" width="70">
                <template #default="{ $index }">
                  <el-button link size="small" type="danger" @click="removeRow(bodyKvItems, $index)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </template>
      </el-tab-pane>

      <!-- Tab: 响应体 -->
      <el-tab-pane label="响应体" name="response">
        <div style="height: 360px">
          <CodeEditor v-model="form.responseBody" language="javascript" :min-height="320" placeholder="请输入响应体示例..." />
        </div>
      </el-tab-pane>

      <!-- Tab: 调试 -->
      <el-tab-pane v-if="isEdit" label="调试" name="debug">
        <el-card shadow="never">
          <div class="debug-env-row">
            <span class="debug-label">选择环境：</span>
            <el-select v-model="debugEnvId" placeholder="选择环境" style="width: 180px">
              <el-option v-for="env in environments" :key="env.id" :value="env.id" :label="env.name" />
            </el-select>
            <el-button type="primary" :loading="debugLoading" @click="sendDebug">发送请求</el-button>
          </div>
          <div class="debug-params">
            <h4>Query 参数</h4>
            <el-table v-if="queryParams.length" :data="queryParams" size="small" border>
              <el-table-column label="参数名" width="180">
                <template #default="{ row }">
                  <span style="font-family: monospace">{{ row.name }}</span>
                </template>
              </el-table-column>
              <el-table-column label="值">
                <template #default="{ row }">
                  <el-input v-model="debugParamValues[row.name]" size="small" placeholder="值" />
                </template>
              </el-table-column>
            </el-table>
            <p v-else class="empty-hint">该接口无 Query 参数</p>
          </div>
          <div v-if="form.httpMethod !== 'GET'" class="debug-params">
            <h4>请求体</h4>
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
            <div v-else-if="debugBodyType === 'raw'" style="height: 240px">
              <CodeEditor v-model="debugBody" :language="debugRawType === 'javascript' ? 'javascript' : debugRawType === 'json' ? 'json' : 'text'" :min-height="200" placeholder="请输入请求体" />
            </div>
            <div v-else-if="debugBodyType === 'graphql'">
              <div class="body-editor-row" style="height: 180px">
                <CodeEditor v-model="debugGraphqlQuery" language="text" :min-height="140" placeholder="请输入 GraphQL Query" />
              </div>
              <div class="body-editor-row" style="height: 120px; margin-top: 8px">
                <CodeEditor v-model="debugGraphqlVariables" language="json" :min-height="80" placeholder="请输入 Variables（JSON）" />
              </div>
            </div>
            <div v-else class="params-section">
              <div class="section-head">
                <h4>{{ debugBodyType === 'form_data' ? '表单参数' : '二进制参数' }}</h4>
                <el-button size="small" @click="addRow(debugBodyKvItems)">+ 添加参数</el-button>
              </div>
              <el-table :data="debugBodyKvItems" size="small" border>
                <el-table-column label="参数名" width="180">
                  <template #default="{ row }"><el-input v-model="row.name" size="small" placeholder="参数名" /></template>
                </el-table-column>
                <el-table-column label="值">
                  <template #default="{ row }"><el-input v-model="row.value" size="small" placeholder="值" /></template>
                </el-table-column>
                <el-table-column label="操作" width="70">
                  <template #default="{ $index }">
                    <el-button link size="small" type="danger" @click="removeRow(debugBodyKvItems, $index)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
          <div v-if="debugResult" class="debug-response">
            <div class="resp-status">
              <span :class="['resp-code', debugResult.success === 1 ? 'ok' : 'err']">
                {{ debugResult.statusCode || (debugResult.success === 1 ? '200 OK' : 'ERROR') }}
              </span>
              <span v-if="debugResult.responseTimeMs" class="resp-meta">耗时: {{ debugResult.responseTimeMs }}ms</span>
              <span v-if="debugResult.responseSize" class="resp-meta">大小: {{ debugResult.responseSize }}</span>
            </div>
            <div v-if="debugResult.requestUrl" class="resp-request-url">
              <span class="resp-meta">请求 URL: {{ debugResult.requestUrl }}</span>
            </div>
            <el-tabs v-model="debugRespTab">
              <el-tab-pane label="响应体" name="body">
                <pre class="resp-body-code">{{ formatDebugBody(debugResult) }}</pre>
              </el-tab-pane>
              <el-tab-pane label="响应头" name="headers">
                <el-table v-if="debugHeaderEntries.length" :data="debugHeaderEntries" size="small" border>
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
                <p v-else class="empty-hint" style="padding: 20px; text-align: center">无响应头信息</p>
              </el-tab-pane>
              <el-tab-pane label="状态信息" name="status">
                <el-descriptions :column="1" border size="small" style="max-width: 400px">
                  <el-descriptions-item label="状态码">{{ debugResult.statusCode ?? (debugResult.success === 1 ? '200 OK' : '-') }}</el-descriptions-item>
                  <el-descriptions-item label="响应时间">{{ debugResult.responseTimeMs ?? '-' }} ms</el-descriptions-item>
                  <el-descriptions-item label="响应大小">{{ debugResult.responseSize ?? '-' }}</el-descriptions-item>
                  <el-descriptions-item label="协议">{{ debugResult.protocol ?? 'HTTP/1.1' }}</el-descriptions-item>
                </el-descriptions>
              </el-tab-pane>
            </el-tabs>
          </div>
        </el-card>
      </el-tab-pane>

      <!-- Tab: 引用关系 -->
      <el-tab-pane v-if="isEdit" label="引用关系" name="refs">
        <div class="refs-header">
          <h4 style="margin: 0; font-size: 14px; font-weight: 600">接口关键字引用</h4>
          <el-tag v-if="references.length" type="primary" size="small">{{ references.length }} 个引用</el-tag>
        </div>
        <el-table v-loading="refsLoading" :data="references" size="small" border style="margin-top: 12px">
          <el-table-column prop="keywordName" label="关键字名称" />
          <el-table-column prop="createdBy" label="创建人" width="120" />
          <el-table-column label="修改时间" width="140">
            <template #default="{ row }">{{ row.updatedAt?.substring(0, 10) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="80">
            <template #default>
              <el-button type="primary" link size="small" @click="router.push(`/project/${projectId}/keywords`)">查看</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!references.length && !refsLoading" description="暂无引用" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.params-section {
  margin-bottom: 24px;
}
.params-section h4 {
  font-size: 14px;
  font-weight: 600;
  margin: 0 0 8px;
}
.refs-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.empty-hint {
  color: #909399;
  font-size: 12px;
  margin: 0;
}
.path-hint {
  color: #909399;
  font-size: 12px;
  line-height: 1.4;
  margin-top: 4px;
}
.empty-state {
  text-align: center;
  padding: 48px;
  color: #909399;
}
.empty-icon {
  font-size: 36px;
  margin-bottom: 8px;
  opacity: 0.4;
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
.debug-env-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.debug-label {
  font-size: 13px;
  color: #606266;
  white-space: nowrap;
}
.debug-params h4 {
  font-size: 13px;
  font-weight: 600;
  margin: 0 0 8px;
}
.debug-response {
  margin-top: 16px;
}
.resp-status {
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
.resp-request-url {
  margin-bottom: 8px;
  word-break: break-all;
}
.resp-body-code {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
  max-height: 360px;
  overflow: auto;
  font-size: 12px;
  font-family: monospace;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
