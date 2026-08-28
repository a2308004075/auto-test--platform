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
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getKeyword, createKeyword, updateKeyword, getKeywordDependencies, getKeywordGroups } from '@/api/keyword'
import { getApis, getApi, getModules } from '@/api/apidoc'
import { useDict } from '@/composables/useDict'
import { usePermission } from '@/composables/usePermission'
import { Refresh } from '@element-plus/icons-vue'
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
const { options: categoryOptions } = useDict('keyword_category')

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
  category: '', tags: '[]',
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
          if (p.name) {
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
const expectedStatusCode = ref('')
const assertionFields = ref<any[]>([])
watch([expectedStatusCode, assertionFields], () => {
  form.responseAssertion = JSON.stringify({
    statusCode: expectedStatusCode.value || undefined,
    fields: assertionFields.value,
  })
}, { deep: true })

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
      category: data.category || '',
      tags: data.tags || '[]',
      testData: data.testData || '[]',
      responseAssertion: data.responseAssertion || '{}',
    })
    testDataRows.value = parseArr(form.testData)
    // 解析断言
    try {
      const assertion = JSON.parse(form.responseAssertion)
      expectedStatusCode.value = assertion.statusCode || ''
      assertionFields.value = Array.isArray(assertion.fields) ? assertion.fields : []
    } catch {
      expectedStatusCode.value = ''
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
        module: modMap.get(mid) || { id: 0, name: '未分类', isSystem: 1 },
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
  if (!form.category) { ElMessage.warning('请选择分类'); activeTab.value = 'basic'; return }
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

// ===== 保存成功弹窗操作 =====
function handleSaveSuccessBack() {
  saveSuccessVisible.value = false
  router.push(`/project/${projectId.value}/keywords`)
}
function handleSaveSuccessContinue() {
  saveSuccessVisible.value = false
  // 重置表单
  Object.assign(form, {
    name: '', apiId: null, groupId: null, description: '',
    category: '', tags: '[]',
    testData: '[]', responseAssertion: '{}',
  })
  testDataRows.value = []
  assertionFields.value = []
  expectedStatusCode.value = ''
  activeTab.value = 'basic'
  router.replace({ hash: '' })
}

// ===== Hash 锚点自动切换 Tab =====
const validTabs = ['basic', 'testdata', 'response', 'refs']
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
})
</script>

<template>
  <div v-loading="loading">
    <EditPageHeader :title="isEdit ? '编辑接口关键字' : '新建接口关键字'">
      <el-button v-if="hasPermission('project:keyword:edit')" type="primary" @click="handleSubmit">保存</el-button>
      <el-button @click="router.back()">取消</el-button>
    </EditPageHeader>

    <div class="keyword-edit-layout">
      <!-- 左侧：选择接口（按分组显示） -->
      <div class="api-selector-panel">
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
              <!-- 第一行：关联接口 -->
              <el-form-item label="关联接口" required>
                <div v-if="currentApi" class="api-info-bar">
                  <el-tag :type="methodColors[currentApi.httpMethod] || 'info'" size="small">{{ currentApi.httpMethod }}</el-tag>
                  <code style="font-size: 13px">{{ apiDisplayPath(currentApi) }}</code>
                  <span style="color: #909399">{{ currentApi.description }}</span>
                  <el-button link type="primary" style="margin-left: auto" @click="router.push(`/project/${projectId}/apis/${currentApi.id}/edit`)">查看接口 →</el-button>
                </div>
              </el-form-item>
              <el-form-item label="关键字名称" required>
                <el-input v-model="form.name" placeholder="请输入关键字名称" />
              </el-form-item>
              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item label="分类" required>
                    <el-select v-model="form.category" placeholder="请选择分类" filterable allow-create default-first-option style="width: 100%">
                      <el-option v-for="opt in categoryOptions" :key="opt.value" :value="opt.value" :label="opt.label" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="关键字分组">
                    <el-select v-model="form.groupId" placeholder="请选择分组" clearable filterable style="width: 100%">
                      <el-option v-for="g in groups" :key="g.id" :value="g.id" :label="g.name" />
                    </el-select>
                  </el-form-item>
                </el-col>
              </el-row>
              <el-form-item label="描述">
                <el-input v-model="form.description" type="textarea" :rows="2" />
              </el-form-item>
            </el-form>
          </el-tab-pane>

          <!-- Tab: 请求参数 -->
          <el-tab-pane label="请求参数" name="testdata">
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

          <!-- Tab: 引用关系 -->
          <el-tab-pane label="引用关系" name="refs" :disabled="!isEdit">
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

    <!-- 保存成功弹窗（创建模式） -->
    <el-dialog v-model="saveSuccessVisible" title="保存接口关键字" width="380px" :close-on-click-modal="false">
      <p style="font-size: 14px; color: #606266; line-height: 1.6; text-align: center;">
        接口关键字 <strong>{{ savedKeywordName }}</strong> 保存成功！
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
</style>
