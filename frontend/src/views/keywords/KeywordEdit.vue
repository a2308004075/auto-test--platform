<!--
 @author HXN
 @date 2026-08-21 15:30
 @description 关键字编辑视图
-->
<script setup lang="ts">
/**
 * 接口关键字编辑/新建 - M5
 * 5 Tab：基础信息 / 关联接口 / 测试数据 / 预期响应 / 引用关系
 * 对齐原型 keyword-edit.html（测试数据/断言为可视化表格编辑，数据序列化为 JSON 存储）
 */
defineOptions({ name: 'KeywordEdit' })
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getKeyword, createKeyword, updateKeyword, getKeywordDependencies, getKeywordGroups, debugKeyword } from '@/api/keyword'
import { getApis, getApi, getModules } from '@/api/apidoc'
import { getEnvironments } from '@/api/environment'
import { useDict } from '@/composables/useDict'
import { usePermission } from '@/composables/usePermission'
import { Refresh, InfoFilled } from '@element-plus/icons-vue'
import EditPageHeader from '@/components/EditPageHeader/index.vue'
import CodeEditor from '@/components/CodeEditor/index.vue'
import { schemaToExampleString } from '@/utils/schemaToExample'
import { formatJson } from '@/utils/jsonFormat'

const route = useRoute()
const router = useRouter()
const { hasPermission } = usePermission()
const projectId = computed(() => Number(route.params.id))
const keywordId = computed(() => Number(route.params.keywordId))
const isEdit = computed(() => !!keywordId.value)

const methodColors: Record<string, string> = { GET: '', POST: 'success', PUT: 'warning', DELETE: 'danger', PATCH: 'info' }
const { options: paramTypeOptions } = useDict('param_type')

const activeTab = ref('basic')
const loading = ref(false)
const apis = ref<any[]>([])
const apiModules = ref<any[]>([])
const groups = ref<any[]>([])

// ===== 保存成功弹窗（创建模式） =====
const saveSuccessVisible = ref(false)
const savedKeywordName = ref('')

const form = reactive({
  name: '', apiId: null as number | null, groupId: null as number | null, description: '',
  tags: '[]',
  testData: '[]', responseAssertion: '{}',
})
const referenceCount = ref(0)
const referenceList = ref<any[]>([])
const referenceLoading = ref(false)

// ===== 测试数据可视化编辑（序列化为 JSON 存入 form.testData） =====
function parseArr(raw?: string): any[] {
  if (!raw) return []
  try { const a = JSON.parse(raw); return Array.isArray(a) ? a : [] } catch { return [] }
}
const testDataRows = ref<any[]>([])
watch(testDataRows, () => { form.testData = JSON.stringify(testDataRows.value) }, { deep: true })

// ===== 从接口定义自动提取参数 =====

// 参数分区 computed
const pathParams = computed(() => testDataRows.value.filter(r => r.in === 'path'))
const queryParams = computed(() => testDataRows.value.filter(r => r.in === 'query'))
const bodyParams = computed(() => testDataRows.value.filter(r => r.in === 'body'))
const bodyRow = computed(() => bodyParams.value.find(r => r.name === '__body__'))
const bodyKvParams = computed(() => bodyParams.value.filter(r => r.name !== '__body__'))

/**
 * 将 JSON Schema（object/array）转为带默认值和行内注释的可读 JSON，非 Schema 原样返回
 */
function schemaToReadableJson(schema: any): string {
  const isSchema = schema && typeof schema === 'object'
    && ((schema.type === 'object' && schema.properties) || (schema.type === 'array' && schema.items))
  if (isSchema) {
    return schemaToExampleString(schema)
  }
  // 非 Schema 格式，原样返回
  return typeof schema === 'string' ? schema : JSON.stringify(schema, null, 2)
}

function extractParamsFromApi(api: any): any[] {
  const params: any[] = []

  // 1. Path 参数：从 URL 中提取 {xxx}（排除 ${...} 环境变量占位符）
  if (api.path) {
    const cleanPath = api.path.replace(/\$\{[^}]*\}/g, '')
    const matches = cleanPath.match(/\{(\w+)\}/g) || []
    matches.forEach((m: string) => {
      params.push({ name: m.slice(1, -1), type: 'string', value: '', description: '路径参数', in: 'path', required: true })
    })
  }

  // 2. Query 参数：从 requestParams JSON 数组中提取
  if (api.requestParams) {
    try {
      const reqParams = JSON.parse(api.requestParams)
      if (Array.isArray(reqParams)) {
        reqParams.forEach((p: any) => {
          // 跳过 in=path 的参数：Path 参数已在上方从 URL {xxx} 提取，避免重复
          if (p.name && p.in !== 'path') {
            params.push({ name: p.name, type: p.type || 'string', value: p.value || '', description: p.description || '', in: 'query', required: !!p.required })
          }
        })
      }
    } catch { /* ignore parse error */ }
  }

  // 3. Body 参数
  const bodyType = api.bodyType || 'raw'
  if (bodyType !== 'none' && api.httpMethod && api.httpMethod !== 'GET') {
    if (bodyType === 'form_data' || bodyType === 'x_www_form_urlencoded') {
      // KV 格式：解析 requestBody 数组
      if (api.requestBody) {
        try {
          const body = JSON.parse(api.requestBody)
          if (Array.isArray(body)) {
            body.forEach((p: any) => {
              if (p.name) {
                params.push({ name: p.name, type: p.type || 'string', value: p.value || '', description: p.description || '', in: 'body', required: !!p.required })
              }
            })
          }
        } catch { /* ignore */ }
      }
    } else if (bodyType === 'raw' || bodyType === 'graphql' || bodyType === 'binary') {
      // JSON/文本/GraphQL/二进制整体：作为单个 Body 字段，预设值从接口文档 requestBody 带入
      let rawType = api.rawType
      if (!rawType) {
        if (bodyType === 'graphql') rawType = 'graphql'
        else if (bodyType === 'binary') rawType = 'binary'
        else rawType = 'json'
      }
      let bodyValue = ''
      if (api.requestBody) {
        try {
          const parsed = JSON.parse(api.requestBody)
          bodyValue = schemaToReadableJson(parsed)
        } catch {
          bodyValue = api.requestBody
        }
      }
      params.push({
        name: '__body__',
        type: rawType === 'json' || rawType === 'graphql' ? 'json' : 'string',
        value: bodyValue,
        description: `请求体 (${rawType})`,
        in: 'body',
        required: true,
      })
    }
  }

  return params
}

async function autoFillParams(apiId: number | null) {
  if (!apiId) return
  try {
    const res: any = await getApi(projectId.value, apiId)
    const api = res.data
    if (api) {
      testDataRows.value = extractParamsFromApi(api)
      if (testDataRows.value.length) {
        ElMessage.success(`已自动填充 ${testDataRows.value.length} 个参数`)
      }
    }
  } catch {
    ElMessage.error('自动填充参数失败，请检查接口定义')
  }
}

async function refreshParams() {
  if (!form.apiId) { ElMessage.warning('请先选择关联接口'); return }
  try {
    const res: any = await getApi(projectId.value, form.apiId)
    const api = res.data
    if (!api) return

    const newParams = extractParamsFromApi(api)
    const existingMap = new Map(testDataRows.value.map(r => [`${r.name}|${r.in}`, r]))

    const merged = newParams.map(p => {
      const key = `${p.name}|${p.in}`
      const existing = existingMap.get(key)
      if (existing) {
        // 保留已填写的预设值，更新元数据
        return { ...p, value: existing.value || p.value }
      }
      return p
    })

    testDataRows.value = merged
    ElMessage.success(`参数已刷新：共 ${merged.length} 个参数`)
  } catch { ElMessage.error('刷新参数失败') }
}

// 新建模式：选择接口后自动填充参数（已整合到 selectApi 函数）

// ===== 断言字段可视化编辑（序列化为 JSON 存入 form.responseAssertion） =====
const expectedStatusCode = ref('200')
const assertionFields = ref<any[]>([])
watch([expectedStatusCode, assertionFields], () => {
  form.responseAssertion = JSON.stringify({
    statusCode: expectedStatusCode.value || '200',
    fields: assertionFields.value,
  })
}, { deep: true, immediate: true })

// ===== 数据加载 =====
async function fetchApis() {
  try {
    const res: any = await getApis(projectId.value, { page: 1, pageSize: 1000 })
    apis.value = res.data?.items || []
  } catch { apis.value = [] }
}

async function fetchApiModules() {
  try {
    const res: any = await getModules(projectId.value)
    apiModules.value = res.data || []
  } catch { apiModules.value = [] }
}

async function fetchGroups() {
  try {
    const res: any = await getKeywordGroups(projectId.value)
    groups.value = res.data || []
  } catch { groups.value = [] }
}

async function fetchKeyword() {
  if (!isEdit.value) return
  loading.value = true
  try {
    const res: any = await getKeyword(projectId.value, keywordId.value)
    const data = res.data
    Object.assign(form, {
      name: data.name || '',
      apiId: data.apiId ?? null,
      groupId: data.groupId ?? null,
      description: data.description || '',
      tags: data.tags || '[]',
      testData: data.testData || '[]',
      responseAssertion: data.responseAssertion || '{}',
    })
    testDataRows.value = parseArr(form.testData)
    // 解析断言
    try {
      const assertion = JSON.parse(form.responseAssertion)
      expectedStatusCode.value = assertion.statusCode || '200'
      assertionFields.value = Array.isArray(assertion.fields) ? assertion.fields : []
    } catch {
      expectedStatusCode.value = '200'
      assertionFields.value = []
    }
    referenceCount.value = data.referenceCount ?? 0
  } catch { ElMessage.error('加载关键字失败') } finally { loading.value = false }
}

async function fetchDependencies() {
  if (!isEdit.value) return
  referenceLoading.value = true
  try {
    const res: any = await getKeywordDependencies(projectId.value, keywordId.value)
    referenceList.value = res.data || []
  } catch { referenceList.value = [] } finally { referenceLoading.value = false }
}

// 关联接口信息
const currentApi = computed(() => apis.value.find((a: any) => a.id === form.apiId))

// 显示路径：服务前缀 + path（去除前导 ${host} 环境变量占位符）
function apiDisplayPath(api: any): string {
  const path = (api.path || '').replace(/^\$\{[^}]*\}/, '')
  const prefix = (api.servicePrefix || '').replace(/\/+$/, '')
  return prefix + path
}

// ===== 左侧接口选择面板（按分组显示） =====
const apiSearch = ref('')
const expandedGroups = ref<number[]>([])

const groupedApis = computed(() => {
  const kw = apiSearch.value.trim().toLowerCase()
  let list = apis.value
  if (kw) {
    list = list.filter((a: any) =>
      a.name?.toLowerCase().includes(kw) ||
      a.path?.toLowerCase().includes(kw) ||
      a.httpMethod?.toLowerCase().includes(kw)
    )
  }
  const modMap = new Map(apiModules.value.map((m: any) => [m.id, m]))
  const map = new Map<number, { module: any, items: any[] }>()
  for (const api of list) {
    const mid = api.moduleId || 0
    if (!map.has(mid)) {
      map.set(mid, {
        module: modMap.get(mid) || { id: 0, name: '未分组', isSystem: 1 },
        items: [],
      })
    }
    map.get(mid)!.items.push(api)
  }
  const result = [...map.values()]
  result.sort((a, b) => {
    if (a.module.isSystem && !b.module.isSystem) return 1
    if (!a.module.isSystem && b.module.isSystem) return -1
    return 0
  })
  return result
})

async function selectApi(api: any) {
  form.apiId = api.id
  // 新建模式：自动带入关键字名称 + 自动填充参数
  if (!isEdit.value) {
    if (!form.name) {
      form.name = api.name
    }
    await autoFillParams(api.id)
  }
}

// ===== 保存 =====
async function handleSubmit() {
  if (!form.name) { ElMessage.warning('请填写关键字名称'); activeTab.value = 'basic'; return }
  if (!form.apiId) { ElMessage.warning('请在左侧选择关联接口'); return }
  try {
    const payload = { ...form, projectId: projectId.value }
    if (isEdit.value) {
      await updateKeyword(projectId.value, keywordId.value, payload)
      ElMessage.success('更新成功')
      router.push(`/project/${projectId.value}/keywords`)
    } else {
      await createKeyword(projectId.value, payload)
      savedKeywordName.value = form.name
      saveSuccessVisible.value = true
    }
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '操作失败') }
}

// ===== 重置表单（新建模式使用） =====
function resetForm() {
  Object.assign(form, {
    name: '', apiId: null, groupId: null, description: '',
    tags: '[]',
    testData: '[]', responseAssertion: '{}',
  })
  testDataRows.value = []
  assertionFields.value = []
  expectedStatusCode.value = '200'
  activeTab.value = 'basic'
  referenceCount.value = 0
  referenceList.value = []
  saveSuccessVisible.value = false
  savedKeywordName.value = ''
  router.replace({ hash: '' })
}

// ===== 保存成功弹窗操作 =====
function handleSaveSuccessBack() {
  saveSuccessVisible.value = false
  router.push(`/project/${projectId.value}/keywords`)
}
function handleSaveSuccessContinue() {
  saveSuccessVisible.value = false
  resetForm()
}

// 路由参数变化时重置/加载数据（解决从编辑页切换到新建页的缓存问题）
watch(() => route.params.keywordId, () => {
  if (!isEdit.value) {
    resetForm()
  }
  fetchKeyword()
  fetchDependencies()
})

// ===== Hash 锚点自动切换 Tab =====
const validTabs = ['basic', 'testdata', 'response', 'refs', 'debug']
watch(() => route.hash, (hash) => {
  const tab = hash.replace('#', '')
  if (tab && validTabs.includes(tab)) {
    activeTab.value = tab
  }
}, { immediate: true })
watch(activeTab, (tab) => {
  const targetHash = `#${tab}`
  if (route.hash !== targetHash) {
    router.replace({ hash: targetHash })
  }
})

onMounted(() => {
  fetchApis()
  fetchApiModules()
  fetchGroups()
  fetchKeyword()
  fetchDependencies()
  loadDebugEnvironments()
})

// ===== 在线调试 =====
const debugLoading = ref(false)
const debugResult = ref<any>(null)
const debugEnvironments = ref<any[]>([])
const debugEnvId = ref<number | null>(null)

async function loadDebugEnvironments() {
  try {
    const res: any = await getEnvironments(projectId.value)
    debugEnvironments.value = res.data || []
  } catch { debugEnvironments.value = [] }
}

// 去除 JSON 字符串中的行内注释，避免后端解析失败
function stripJsonComments(json: string): string {
  return json.replace(/\/\/.*$/gm, '')
}

async function executeDebug() {
  if (!isEdit.value) { ElMessage.warning('请先保存关键字'); return }
  if (!form.apiId) { ElMessage.warning('请先选择关联接口'); return }
  if (!debugEnvId.value) { ElMessage.warning('请选择执行环境'); return }
  debugLoading.value = true
  debugResult.value = null
  try {
    const testData = testDataRows.value.map((p: any) => {
      if (p.in === 'body' && p.name === '__body__' && typeof p.value === 'string') {
        return { ...p, value: stripJsonComments(p.value) }
      }
      return p
    })
    const res: any = await debugKeyword(projectId.value, keywordId.value, {
      environmentId: debugEnvId.value,
      testData: JSON.stringify(testData),
    })
    const data = res.data || {}
    let responseBody: any = data.responseBody
    if (typeof responseBody === 'string') {
      try { responseBody = JSON.parse(responseBody) } catch { responseBody = { raw: data.responseBody } }
    }
    debugResult.value = {
      success: data.success === 1,
      statusCode: data.statusCode ?? 0,
      response: responseBody,
      duration: data.responseTimeMs ?? 0,
    }
  } catch (e: any) {
    debugResult.value = { success: false, statusCode: 500, response: { error: e?.response?.data?.message || e?.message || '请求失败' }, duration: 0 }
  } finally { debugLoading.value = false }
}

function parseAssertions(raw?: string): any[] {
  if (!raw) return []
  try {
    const obj = JSON.parse(raw)
    return Array.isArray(obj?.fields) ? obj.fields : []
  } catch { return [] }
}

function getNestedValue(obj: any, path: string): any {
  if (!obj || !path) return undefined
  const parts = path.split('.')
  let current = obj
  for (const part of parts) {
    if (current == null) return undefined
    current = current[part]
  }
  return current
}

const debugAssertions = computed(() => {
  if (!debugResult.value) return []
  const assertions = parseAssertions(form.responseAssertion)
  return assertions.map((a: any) => {
    const actual = getNestedValue(debugResult.value.response, a.path)
    const expectedValue = a.expected || '非空即可'
    const actualValue = actual != null ? String(actual) : '--'
    const pass = expectedValue === '非空即可' ? actual != null : actualValue === expectedValue
    return { ...a, actual: actualValue, pass }
  })
})

const debugEnvName = computed(() => {
  if (!debugEnvId.value) return ''
  const env = debugEnvironments.value.find((e: any) => e.id === debugEnvId.value)
  return env?.name || ''
})

function syntaxHighlightJSON(json: string): string {
  return json
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/("(\\u[\da-fA-F]{4}|\\[^u]|[^\\"])*"(\s*:)?|\b(true|false|null)\b|-?\d+(?:\.\d*)?(?:[eE][+-]?\d+)?)/g, (match) => {
      let cls = 'json-num'
      if (/^"/.test(match)) {
        cls = /:$/.test(match) ? 'json-key' : 'json-str'
      } else if (/true|false/.test(match)) {
        cls = 'json-bool'
      } else if (/null/.test(match)) {
        cls = 'json-null'
      }
      return `<span class="${cls}">${match}</span>`
    })
}

const highlightedDebugResponse = computed(() => {
  if (!debugResult.value) return ''
  return syntaxHighlightJSON(JSON.stringify(debugResult.value.response, null, 2))
})

// ===== 查看接口详情弹窗 =====
const apiDetailVisible = ref(false)

const apiDetailQueryParams = computed(() => {
  if (!currentApi.value) return []
  return parseArr(currentApi.value.requestParams).filter((p: any) => p.in !== 'path')
})

const apiDetailHeaders = computed(() => parseArr(currentApi.value?.headers))

function looksLikeSchema(text: any): boolean {
  if (!text || typeof text !== 'object') return false
  if (text.type === 'object' && text.properties) return true
  if (text.type === 'array' && text.items) return true
  return false
}

const apiDetailFormattedBody = computed(() => {
  if (!currentApi.value?.requestBody) return ''
  try {
    const parsed = JSON.parse(currentApi.value.requestBody)
    if (typeof parsed === 'string') return parsed
    if (looksLikeSchema(parsed)) return schemaToExampleString(parsed)
    return JSON.stringify(parsed, null, 2)
  } catch {
    return currentApi.value.requestBody
  }
})

const apiDetailFormattedResponse = computed(() => {
  if (!currentApi.value?.responseBody) return ''
  try {
    const parsed = JSON.parse(currentApi.value.responseBody)
    if (typeof parsed === 'string') return parsed
    if (looksLikeSchema(parsed)) return schemaToExampleString(parsed)
    return JSON.stringify(parsed, null, 2)
  } catch {
    return currentApi.value.responseBody
  }
})

function openApiDetail() {
  apiDetailVisible.value = true
}
</script>

<template>
  <div v-loading="loading">
    <EditPageHeader :title="isEdit ? '编辑接口关键字' : '新建接口关键字'" :show-back="false">
      <el-button v-if="hasPermission('project:keyword:edit')" type="primary" @click="handleSubmit">保存</el-button>
      <el-button @click="router.back()">取消</el-button>
    </EditPageHeader>

    <div class="keyword-edit-layout">
      <!-- 左侧：选择接口（按分组显示，仅新建时显示） -->
      <div v-if="!isEdit" class="api-selector-panel">
        <div class="panel-header">
          <h3>选择接口</h3>
          <el-input v-model="apiSearch" placeholder="搜索接口..." size="small" clearable prefix-icon="Search" style="margin-top: 8px" />
        </div>
        <div class="api-list">
          <div v-for="group in groupedApis" :key="group.module.id" class="api-group">
            <div
              class="api-group-header"
              @click="expandedGroups.includes(group.module.id)
                ? expandedGroups.splice(expandedGroups.indexOf(group.module.id), 1)
                : expandedGroups.push(group.module.id)"
            >
              <span class="group-arrow" :class="{ expanded: expandedGroups.includes(group.module.id) }">&#9656;</span>
              <span class="group-name">{{ group.module.name }}</span>
              <span class="group-count">{{ group.items.length }}</span>
            </div>
            <div v-show="expandedGroups.includes(group.module.id)" class="api-group-items">
              <div
                v-for="api in group.items" :key="api.id"
                class="api-item"
                :class="{ active: form.apiId === api.id }"
                @click="selectApi(api)"
              >
                <div class="api-item-top">
                  <el-tag :type="methodColors[api.httpMethod] || 'info'" size="small" effect="dark">{{ api.httpMethod }}</el-tag>
                  <span class="api-item-name">{{ api.name }}</span>
                </div>
                <div class="api-item-path">{{ apiDisplayPath(api) }}</div>
              </div>
            </div>
          </div>
          <div v-if="!groupedApis.length" class="api-list-empty">
            {{ apis.length ? '无匹配接口' : '加载中...' }}
          </div>
        </div>
      </div>

      <!-- 右侧：详情 Tabs -->
      <div class="detail-panel">
        <el-tabs v-model="activeTab">
          <!-- Tab: 基础信息 -->
          <el-tab-pane label="基础信息" name="basic">
            <el-form label-position="top">
              <!-- 关联接口名称 -->
              <div style="position: relative">
                <el-form-item label="关联接口名称" required>
                  <el-input :model-value="currentApi?.name || ''" disabled placeholder="选择接口后自动显示" />
                </el-form-item>
                <el-button v-if="currentApi" link type="primary" style="position: absolute; top: 0; right: 0" @click="openApiDetail">查看接口 →</el-button>
              </div>
              <!-- 关联接口 -->
              <el-form-item label="关联接口" required>
                <el-input
                  :model-value="currentApi ? `${currentApi.httpMethod} ${apiDisplayPath(currentApi)}` : ''"
                  disabled
                  placeholder="选择接口后自动显示"
                />
              </el-form-item>
              <el-form-item label="关键字名称" required>
                <el-input v-model="form.name" placeholder="请输入关键字名称" />
              </el-form-item>
              <el-form-item label="分组">
                <el-select v-model="form.groupId" placeholder="请选择分组" clearable filterable style="width: 100%">
                  <el-option v-for="g in groups.filter((g) => g.isSystem !== 1)" :key="g.id" :value="g.id" :label="g.name" />
                </el-select>
              </el-form-item>
              <el-form-item label="描述">
                <el-input v-model="form.description" type="textarea" :rows="2" />
              </el-form-item>
            </el-form>
          </el-tab-pane>

          <!-- Tab: 请求参数 -->
          <el-tab-pane label="请求参数" name="testdata">
            <!-- 参数接收说明：标题 + 悬浮查看详情 -->
            <el-alert type="info" :closable="false" class="param-receive-alert">
              <template #title>
                <span>入参与出参说明</span>
                <el-tooltip placement="bottom" effect="light">
                  <template #content>
                    <div style="max-width: 400px; line-height: 1.8; padding: 2px 0;">
                      <div style="font-weight: 600; margin-bottom: 6px;">入参：外部传入的值会替换 <code style="background:#f5f5f5; padding:1px 4px; border-radius:3px;">$ref{参数名}</code></div>
                      <div>• 在请求头、请求体或路径中写 <code style="background:#f5f5f5; padding:1px 4px; border-radius:3px;">$ref{参数名}</code></div>
                      <div>• 调用方传入同名参数即可替换</div>
                      <div>• 没传时先用下方预设值，再没有则为空</div>
                      <div style="margin-top: 12px; font-weight: 600; margin-bottom: 6px;">出参：把返回值存成变量供后续步骤使用</div>
                      <div>• 在参数中配置 <code style="background:#f5f5f5; padding:1px 4px; border-radius:3px;">output</code>：<code style="background:#f5f5f5; padding:1px 4px; border-radius:3px;">"变量名": "$.json路径"</code></div>
                      <div>• 后续步骤用 <code style="background:#f5f5f5; padding:1px 4px; border-radius:3px;">${变量名}</code> 引用</div>
                      <div style="margin-top: 6px; color: #999;">示例：配置 <code style="background:#f5f5f5; padding:1px 4px; border-radius:3px;">"token": "$.data.token"</code>，后续用 <code style="background:#f5f5f5; padding:1px 4px; border-radius:3px;">${token}</code> 取值</div>
                    </div>
                  </template>
                  <el-icon class="param-help-icon"><InfoFilled /></el-icon>
                </el-tooltip>
              </template>
            </el-alert>

            <!-- 刷新按钮 -->
            <div style="display: flex; justify-content: flex-end; margin-bottom: 12px">
              <el-button v-if="form.apiId" size="small" type="primary" plain @click="refreshParams">
                <el-icon style="margin-right: 2px"><Refresh /></el-icon>从接口刷新参数
              </el-button>
            </div>

            <template v-if="form.apiId">
              <!-- Path 参数 -->
              <div class="params-section">
                <h4>Path 参数</h4>
                <el-table v-if="pathParams.length" :data="pathParams" size="small" border>
                  <el-table-column label="参数名" width="160">
                    <template #default="{ row }">
                      <span style="font-family: monospace; color: #e6a23c">{{ '{' + row.name + '}' }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column label="类型" width="110">
                    <template #default="{ row }">
                      <el-select v-model="row.type" size="small">
                        <el-option v-for="t in paramTypeOptions" :key="t.value" :value="t.value" :label="t.label" />
                      </el-select>
                    </template>
                  </el-table-column>
                  <el-table-column label="预设值" min-width="200">
                    <template #default="{ row }">
                      <el-input v-model="row.value" size="small" placeholder="预设值" />
                    </template>
                  </el-table-column>
                  <el-table-column label="说明" width="160">
                    <template #default="{ row }"><el-input v-model="row.description" size="small" placeholder="说明" /></template>
                  </el-table-column>
                </el-table>
                <p v-else class="empty-hint">该接口无 Path 参数</p>
              </div>

              <!-- Query 参数 -->
              <div class="params-section">
                <div class="section-head">
                  <h4>Query 参数</h4>
                  <el-button size="small" @click="testDataRows.push({ name: '', type: 'string', value: '', description: '', in: 'query' })">+ 添加参数</el-button>
                </div>
                <el-table v-if="queryParams.length" :data="queryParams" size="small" border>
                  <el-table-column label="参数名" width="160">
                    <template #default="{ row }">
                      <el-input v-model="row.name" size="small" placeholder="参数名" />
                    </template>
                  </el-table-column>
                  <el-table-column label="类型" width="110">
                    <template #default="{ row }">
                      <el-select v-model="row.type" size="small">
                        <el-option v-for="t in paramTypeOptions" :key="t.value" :value="t.value" :label="t.label" />
                      </el-select>
                    </template>
                  </el-table-column>
                  <el-table-column label="预设值" min-width="200">
                    <template #default="{ row }">
                      <el-input v-model="row.value" size="small" placeholder="预设值" />
                    </template>
                  </el-table-column>
                  <el-table-column label="说明" width="160">
                    <template #default="{ row }"><el-input v-model="row.description" size="small" placeholder="说明" /></template>
                  </el-table-column>
                  <el-table-column label="操作" width="60" align="center">
                    <template #default="{ row }">
                      <el-button link size="small" type="danger" @click="testDataRows.splice(testDataRows.indexOf(row), 1)">删除</el-button>
                    </template>
                  </el-table-column>
                </el-table>
                <p v-else class="empty-hint">该接口无 Query 参数，可手动添加</p>
              </div>

              <!-- 请求体 -->
              <div class="params-section">
                <h4>请求体</h4>
                <!-- raw/json body: 富文本编辑器 + 格式化；其他类型: textarea -->
                <template v-if="bodyRow">
                  <div v-if="bodyRow.type === 'json'">
                    <div style="display: flex; justify-content: flex-end; margin-bottom: 8px">
                      <el-button size="small" @click="bodyRow.value = formatJson(bodyRow.value)">格式化</el-button>
                    </div>
                    <div style="height: 240px">
                      <CodeEditor v-model="bodyRow.value" language="json" :min-height="200" placeholder='如 {"key": "value"}' />
                    </div>
                  </div>
                  <el-input v-else v-model="bodyRow.value" type="textarea" :rows="8" placeholder='如 {"key": "value"}' style="font-family: monospace" />
                </template>
                <!-- form body: KV 表格 -->
                <template v-else-if="bodyKvParams.length">
                  <el-table :data="bodyKvParams" size="small" border>
                    <el-table-column label="参数名" width="160">
                      <template #default="{ row }">
                        <el-input v-model="row.name" size="small" placeholder="参数名" />
                      </template>
                    </el-table-column>
                    <el-table-column label="类型" width="110">
                      <template #default="{ row }">
                        <el-select v-model="row.type" size="small">
                          <el-option v-for="t in paramTypeOptions" :key="t.value" :value="t.value" :label="t.label" />
                        </el-select>
                      </template>
                    </el-table-column>
                    <el-table-column label="预设值" min-width="200">
                      <template #default="{ row }">
                        <el-input v-model="row.value" size="small" placeholder="预设值" />
                      </template>
                    </el-table-column>
                    <el-table-column label="说明" width="160">
                      <template #default="{ row }"><el-input v-model="row.description" size="small" placeholder="说明" /></template>
                    </el-table-column>
                    <el-table-column label="操作" width="60" align="center">
                      <template #default="{ row }">
                        <el-button link size="small" type="danger" @click="testDataRows.splice(testDataRows.indexOf(row), 1)">删除</el-button>
                      </template>
                    </el-table-column>
                  </el-table>
                </template>
                <p v-else class="empty-hint">该接口无请求体</p>
              </div>
            </template>

            <div v-else class="empty-state" style="padding: 48px">
              <p class="empty-hint">请先在左侧选择关联接口</p>
            </div>
          </el-tab-pane>

          <!-- Tab: 预期响应 -->
          <el-tab-pane label="预期响应" name="response">
            <div class="params-section">
              <div class="status-code-header">
                <h4 style="margin: 0">预期状态码</h4>
                <el-input v-model="expectedStatusCode" placeholder="如 200" style="width: 80px" />
                <el-button size="small" @click="assertionFields.push({ path: '', expected: '', description: '' })">+ 添加断言字段</el-button>
              </div>
              <el-table :data="assertionFields" size="small" border style="margin-top: 12px">
                <el-table-column label="字段路径" width="200">
                  <template #default="{ row }"><el-input v-model="row.path" size="small" placeholder="如 data.token" style="font-family: monospace" /></template>
                </el-table-column>
                <el-table-column label="预期值" width="200">
                  <template #default="{ row }"><el-input v-model="row.expected" size="small" placeholder="预期值" /></template>
                </el-table-column>
                <el-table-column label="说明">
                  <template #default="{ row }"><el-input v-model="row.description" size="small" placeholder="说明" /></template>
                </el-table-column>
                <el-table-column label="操作" width="70">
                  <template #default="{ $index }">
                    <el-button link size="small" type="danger" @click="assertionFields.splice($index, 1)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </el-tab-pane>

          <!-- Tab: 调试 -->
          <el-tab-pane label="调试" name="debug">
            <template v-if="isEdit">
              <!-- 环境选择 + 执行按钮 -->
              <div class="debug-env-row">
                <span style="font-size: 13px; color: #606266">执行环境：</span>
                <el-select v-model="debugEnvId" placeholder="选择环境" style="width: 160px" size="small">
                  <el-option v-for="env in debugEnvironments" :key="env.id" :value="env.id" :label="env.name" />
                </el-select>
                <el-button type="primary" :loading="debugLoading" @click="executeDebug" style="margin-left: auto">
                  {{ debugLoading ? '请求中...' : (debugResult ? '重新发送' : '发送请求') }}
                </el-button>
              </div>
              <!-- 请求参数 -->
              <div class="debug-section-title">请求参数</div>
              <div v-if="testDataRows.length" style="margin-bottom: 16px">
                <!-- Path 参数 -->
                <div v-if="pathParams.length" class="debug-param-section">
                  <h4>Path 参数</h4>
                  <el-table :data="pathParams" size="small" border>
                    <el-table-column label="参数名" width="150">
                      <template #default="{ row }"><span class="debug-path-name">{{ '{' + row.name + '}' }}</span></template>
                    </el-table-column>
                    <el-table-column label="类型" width="90">
                      <template #default="{ row }"><el-tag size="small">{{ row.type || 'string' }}</el-tag></template>
                    </el-table-column>
                    <el-table-column label="测试值" min-width="180">
                      <template #default="{ row }"><el-input v-model="row.value" size="small" placeholder="测试值" /></template>
                    </el-table-column>
                    <el-table-column label="说明" min-width="110">
                      <template #default="{ row }"><span class="debug-param-desc">{{ row.description || '--' }}</span></template>
                    </el-table-column>
                  </el-table>
                </div>
                <!-- Query 参数 -->
                <div v-if="queryParams.length" class="debug-param-section">
                  <h4>Query 参数</h4>
                  <el-table :data="queryParams" size="small" border>
                    <el-table-column label="参数名" width="150">
                      <template #default="{ row }"><code>{{ row.name }}</code></template>
                    </el-table-column>
                    <el-table-column label="类型" width="90">
                      <template #default="{ row }"><el-tag size="small">{{ row.type || 'string' }}</el-tag></template>
                    </el-table-column>
                    <el-table-column label="测试值" min-width="180">
                      <template #default="{ row }"><el-input v-model="row.value" size="small" placeholder="测试值" /></template>
                    </el-table-column>
                    <el-table-column label="说明" min-width="110">
                      <template #default="{ row }"><span class="debug-param-desc">{{ row.description || '--' }}</span></template>
                    </el-table-column>
                  </el-table>
                </div>
                <!-- 请求体 -->
                <div v-if="bodyRow || bodyKvParams.length" class="debug-param-section">
                  <h4>请求体</h4>
                  <template v-if="bodyRow">
                    <div v-if="bodyRow.type === 'json'" style="display: flex; justify-content: flex-end; margin-bottom: 8px">
                      <el-button size="small" @click="bodyRow.value = formatJson(bodyRow.value)">格式化</el-button>
                    </div>
                    <div v-if="bodyRow.type === 'json'" style="height: 200px">
                      <CodeEditor v-model="bodyRow.value" language="json" :min-height="160" placeholder='如 {"key": "value"}' />
                    </div>
                    <el-input v-else v-model="bodyRow.value" type="textarea" :rows="6" placeholder='如 {"key": "value"}' style="font-family: monospace" />
                  </template>
                  <el-table v-else :data="bodyKvParams" size="small" border>
                    <el-table-column label="参数名" width="150">
                      <template #default="{ row }"><code>{{ row.name }}</code></template>
                    </el-table-column>
                    <el-table-column label="类型" width="90">
                      <template #default="{ row }"><el-tag size="small">{{ row.type || 'string' }}</el-tag></template>
                    </el-table-column>
                    <el-table-column label="测试值" min-width="180">
                      <template #default="{ row }"><el-input v-model="row.value" size="small" placeholder="测试值" /></template>
                    </el-table-column>
                    <el-table-column label="说明" min-width="110">
                      <template #default="{ row }"><span class="debug-param-desc">{{ row.description || '--' }}</span></template>
                    </el-table-column>
                  </el-table>
                </div>
              </div>
              <p v-else class="debug-param-desc" style="margin-bottom: 16px">该关键字无请求参数</p>
              <!-- 响应结果 -->
              <div v-if="debugLoading || debugResult" class="debug-result-section">
                <div v-if="debugLoading && !debugResult" class="debug-result-header loading">
                  <span class="debug-spin-icon">&#8635;</span>
                  <span>正在发送请求...</span>
                </div>
                <template v-else-if="debugResult">
                  <div :class="['debug-result-header', debugResult.success ? 'success' : 'fail']">
                    <span style="font-weight: 600">{{ debugResult.success ? '&#10003;' : '&#10007;' }} {{ debugResult.statusCode }}</span>
                    <span>{{ debugResult.success ? '请求成功' : '请求失败' }}</span>
                    <span style="margin-left: auto; color: #909399">耗时 {{ debugResult.duration }}ms</span>
                    <span style="color: #909399">环境：{{ debugEnvName }}</span>
                  </div>
                  <div class="debug-section-title" style="margin-top: 10px">响应体</div>
                  <pre class="debug-response-box" v-html="highlightedDebugResponse"></pre>
                  <div v-if="debugAssertions.length" class="debug-assertions">
                    <div class="debug-section-title" style="margin-top: 12px">断言结果</div>
                    <el-table :data="debugAssertions" size="small" border>
                      <el-table-column label="断言字段" width="200">
                        <template #default="{ row }"><code>{{ row.path }}</code></template>
                      </el-table-column>
                      <el-table-column label="预期值" width="120">
                        <template #default="{ row }">{{ row.expected || '非空即可' }}</template>
                      </el-table-column>
                      <el-table-column label="实际值">
                        <template #default="{ row }">{{ row.actual }}</template>
                      </el-table-column>
                      <el-table-column label="结果" width="80">
                        <template #default="{ row }">
                          <span :style="{ color: row.pass ? '#67c23a' : '#f56c6c', fontWeight: 500 }">
                            {{ row.pass ? '&#10003; 通过' : '&#10007; 失败' }}
                          </span>
                        </template>
                      </el-table-column>
                    </el-table>
                  </div>
                </template>
              </div>
            </template>
            <div v-else class="empty-state" style="padding: 48px">
              <p class="empty-hint">新建模式下请先保存关键字，保存后可进入调试</p>
            </div>
          </el-tab-pane>

          <!-- Tab: 引用关系 -->
          <el-tab-pane v-if="isEdit" label="引用关系" name="refs">
            <div class="refs-header">
              <h4 style="margin: 0; font-size: 14px; font-weight: 600">引用关系详情</h4>
              <el-tag type="primary" size="small">{{ referenceCount }} 个引用</el-tag>
            </div>
            <el-table v-loading="referenceLoading" :data="referenceList" size="small">
              <el-table-column label="引用类型" width="120">
                <template #default="{ row }">
                  <el-tag :type="row.refType === 'ACTION' ? 'primary' : 'success'" size="small">
                    {{ row.refType === 'ACTION' ? 'Action关键字' : '测试用例' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="refName" label="名称" min-width="200">
                <template #default="{ row }">
                  <el-button link type="primary" @click="router.push(`/project/${projectId}/${row.refType === 'ACTION' ? 'actions' : 'cases'}/${row.refId}/edit`)">
                    {{ row.refName }}
                  </el-button>
                </template>
              </el-table-column>
              <el-table-column prop="refDescription" label="描述" min-width="200" show-overflow-tooltip>
                <template #default="{ row }">
                  {{ row.refDescription || '-' }}
                </template>
              </el-table-column>
              <template #empty>
                <div style="padding: 24px 0; color: #c0c4cc; font-size: 13px">暂无引用关系</div>
              </template>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>

    <!-- 接口详情弹窗 -->
    <el-dialog v-model="apiDetailVisible" title="接口详情" width="780px">
      <template v-if="currentApi">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="接口名称">{{ currentApi.name }}</el-descriptions-item>
          <el-descriptions-item label="HTTP 方法">
            <el-tag :type="methodColors[currentApi.httpMethod] || 'info'" size="small">{{ currentApi.httpMethod }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="接口路径" :span="2">
            <code style="font-size: 13px">{{ apiDisplayPath(currentApi) }}</code>
          </el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ currentApi.description || '-' }}</el-descriptions-item>
        </el-descriptions>

        <!-- Header 参数 -->
        <div class="detail-section">
          <h4>Header 参数</h4>
          <el-table v-if="apiDetailHeaders.length" :data="apiDetailHeaders" size="small" border>
            <el-table-column prop="name" label="参数名" width="160" />
            <el-table-column prop="type" label="类型" width="80" />
            <el-table-column label="必填" width="60"><template #default="{ row }">{{ row.required ? '是' : '否' }}</template></el-table-column>
            <el-table-column prop="value" label="值" />
            <el-table-column prop="description" label="说明" />
          </el-table>
          <p v-else class="empty-hint">无 Header 参数</p>
        </div>

        <!-- 请求参数 -->
        <div class="detail-section">
          <h4>请求参数</h4>
          <el-table v-if="apiDetailQueryParams.length" :data="apiDetailQueryParams" size="small" border>
            <el-table-column prop="name" label="参数名" width="160" />
            <el-table-column prop="type" label="类型" width="80" />
            <el-table-column label="必填" width="60"><template #default="{ row }">{{ row.required ? '是' : '否' }}</template></el-table-column>
            <el-table-column prop="description" label="说明" />
          </el-table>
          <p v-else class="empty-hint">无 Query 参数</p>
        </div>

        <!-- 请求体 -->
        <div v-if="currentApi.httpMethod !== 'GET'" class="detail-section">
          <h4>请求体</h4>
          <pre v-if="apiDetailFormattedBody" class="detail-code-block">{{ apiDetailFormattedBody }}</pre>
          <p v-else class="empty-hint">无请求体</p>
        </div>

        <!-- 响应体 -->
        <div class="detail-section">
          <h4>响应体</h4>
          <pre v-if="apiDetailFormattedResponse" class="detail-code-block">{{ apiDetailFormattedResponse }}</pre>
          <p v-else class="empty-hint">无响应体</p>
        </div>
      </template>
      <template #footer>
        <el-button @click="apiDetailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 保存成功弹窗（创建模式） -->
    <el-dialog v-model="saveSuccessVisible" class="save-success-dialog" title="保存成功" width="500px" :close-on-click-modal="false">
      <p style="font-size: 14px; color: #606266; line-height: 1.6; text-align: center;">
        <strong>{{ savedKeywordName }}</strong> 保存成功！
      </p>
      <template #footer>
        <el-button @click="handleSaveSuccessBack">返回列表</el-button>
        <el-button type="primary" @click="handleSaveSuccessContinue">继续新建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.keyword-edit-layout {
  display: flex;
  gap: 0;
  height: calc(100vh - 120px);
  margin: -8px -20px 0;
}

/* 左侧：接口选择面板 */
.api-selector-panel {
  width: 320px;
  min-width: 320px;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  background: #fafafa;
}
.panel-header {
  padding: 16px 16px 12px;
  border-bottom: 1px solid #ebeef5;
}
.panel-header h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
}
.api-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}
.api-item {
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
  margin-bottom: 4px;
  border: 1px solid transparent;
  transition: all 0.15s;
}
.api-item:hover {
  background: #ecf5ff;
}
.api-item.active {
  background: #ecf5ff;
  border-color: #409eff;
}
.api-item-top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}
.api-item-name {
  font-size: 13px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.api-item-path {
  font-size: 12px;
  color: #909399;
  font-family: monospace;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.api-list-empty {
  text-align: center;
  padding: 40px 16px;
  color: #909399;
  font-size: 13px;
}

/* 分组折叠 */
.api-group {
  margin-bottom: 2px;
}
.api-group-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  border-radius: 4px;
  user-select: none;
}
.api-group-header:hover {
  background: #f0f0f0;
}
.group-arrow {
  font-size: 10px;
  color: #909399;
  transition: transform 0.15s;
  display: inline-block;
}
.group-arrow.expanded {
  transform: rotate(90deg);
}
.group-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.group-count {
  font-size: 11px;
  color: #909399;
  font-weight: normal;
}
.api-group-items {
  padding-left: 8px;
}

/* 右侧：详情面板 */
.detail-panel {
  flex: 1;
  overflow-y: auto;
  padding: 0 20px 20px;
  min-width: 0;
}

.params-section {
  margin-bottom: 24px;
}
.params-section h4 {
  font-size: 14px;
  font-weight: 600;
  margin: 0 0 8px;
}
.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}
.status-code-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}
.api-info-bar {
  padding: 10px 12px;
  background: #fafafa;
  border-radius: 4px;
  display: flex;
  gap: 12px;
  font-size: 13px;
  align-items: center;
  width: 100%;
}
.param-receive-alert {
  margin-bottom: 12px;
}
.param-receive-alert :deep(p) {
  margin: 4px 0;
  font-size: 12px;
  line-height: 1.6;
}
.param-receive-alert :deep(code) {
  background: #f0f2f5;
  padding: 0 4px;
  border-radius: 3px;
  font-size: 12px;
}
.param-help-icon {
  color: #909399;
  cursor: pointer;
  font-size: 15px;
  margin-left: 4px;
  vertical-align: middle;
}
.empty-hint {
  color: #909399;
  font-size: 12px;
  margin: 0;
}
.empty-state {
  text-align: center;
  padding: 48px;
  color: #909399;
}
.refs-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}
.detail-section {
  margin-top: 16px;
}
.detail-section h4 {
  font-size: 14px;
  font-weight: 600;
  margin: 0 0 8px;
}
.detail-code-block {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
  max-height: 240px;
  overflow: auto;
  font-size: 12px;
  font-family: monospace;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}

/* 在线调试 */
.debug-env-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}
.debug-section-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}
.debug-param-section {
  margin-bottom: 16px;
}
.debug-param-section h4 {
  font-size: 13px;
  font-weight: 600;
  margin: 0 0 8px;
}
.debug-path-name {
  font-family: monospace;
  color: #e6a23c;
}
.debug-param-desc {
  color: #909399;
  font-size: 12px;
}
.debug-result-section {
  margin-top: 4px;
}
.debug-result-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 4px;
  font-size: 13px;
  margin-bottom: 10px;
}
.debug-result-header.success {
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  color: #15803d;
}
.debug-result-header.fail {
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #dc2626;
}
.debug-result-header.loading {
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  color: #2563eb;
}
.debug-spin-icon {
  display: inline-block;
  animation: debug-spin 0.8s linear infinite;
  font-size: 14px;
}
@keyframes debug-spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
.debug-response-box {
  background: #1e1e2e;
  color: #cdd6f4;
  border-radius: 4px;
  padding: 12px 14px;
  font-family: 'Consolas', monospace;
  font-size: 12px;
  line-height: 1.6;
  max-height: 260px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
}
.debug-response-box :deep(.json-key) { color: #89b4fa; }
.debug-response-box :deep(.json-str) { color: #a6e3a1; }
.debug-response-box :deep(.json-num) { color: #fab387; }
.debug-response-box :deep(.json-bool) { color: #cba6f7; }
.debug-response-box :deep(.json-null) { color: #9399b2; }
.debug-assertions {
  margin-top: 10px;
}
</style>
