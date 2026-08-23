<!--
 @author HXN
 @date 2026-08-18 17:31
 @description 关键字列表视图
-->
<script setup lang="ts">
/**
 * 接口关键字列表 - M5
 * 左侧复用接口分组树 + 右侧高级搜索 + 批量操作(删除/修改分组) + 表字段调整 + 在线调试 + 智能分页
 * 对齐原型 keyword-list.html
 */
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getKeywords, deleteKeyword, generateKeyword, updateKeyword, debugKeyword } from '@/api/keyword'
import { getModules, getApis } from '@/api/apidoc'
import { getEnvironments } from '@/api/environment'
import PageHeader from '@/components/PageHeader/index.vue'
import ProSearchCard from '@/components/ProSearchCard/index.vue'
import BatchBar from '@/components/BatchBar/index.vue'
import ColumnSettings, { type ColumnItem } from '@/components/ColumnSettings/index.vue'
import ProPagination from '@/components/ProPagination/index.vue'
import { usePermission } from '@/composables/usePermission'

const route = useRoute()
const router = useRouter()
const { hasPermission } = usePermission()
const projectId = computed(() => Number(route.params.id))

const methodColors: Record<string, string> = { GET: '', POST: 'success', PUT: 'warning', DELETE: 'danger', PATCH: 'info' }

// ===== 列表数据 =====
const loading = ref(false)
const list = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const selectedRows = ref<any[]>([])

// ===== 搜索条件 =====
const search = reactive({ keyword: '', apiGroup: '', apiName: '', apiPath: '' })

// ===== 分组树（复用接口分组） =====
const modules = ref<any[]>([])
const activeModuleId = ref<number>(0) // 0 = 全部
const moduleMap = computed<Record<number, any>>(() => {
  const m: Record<number, any> = {}
  modules.value.forEach((mod) => { m[mod.id] = mod })
  return m
})
const moduleTree = computed(() => {
  const allModules = modules.value
  const userGroups = allModules.filter((m: any) => m.isSystem !== 1)
  const systemGroups = allModules.filter((m: any) => m.isSystem === 1 && m.name !== '全部')
  const buildTree = (parentId: number | null): any[] =>
    userGroups
      .filter((m: any) => (m.parentId ?? null) === parentId)
      .map((m: any) => ({ ...m, children: buildTree(m.id) }))
  return [
    { id: 0, name: '全部', isSystem: 1, apiCount: pagination.total, children: [] },
    ...systemGroups.map((g: any) => ({ ...g, apiCount: g.apiCount ?? 0, children: [] })),
    ...buildTree(null),
  ]
})
function onModuleNodeClick(data: any) {
  activeModuleId.value = data.id
  pagination.current = 1
  fetchList()
}

async function fetchModules() {
  try {
    const res: any = await getModules(projectId.value)
    modules.value = res.data || []
  } catch { modules.value = [] }
}

async function fetchList() {
  loading.value = true
  try {
    const res: any = await getKeywords(projectId.value, {
      moduleId: activeModuleId.value || undefined,
      keyword: search.keyword || undefined,
      apiGroup: search.apiGroup || undefined,
      apiName: search.apiName || undefined,
      apiPath: search.apiPath || undefined,
      page: pagination.current, pageSize: pagination.pageSize,
    })
    list.value = res.data?.items || []
    pagination.total = res.data?.total || 0
  } catch { list.value = [] } finally { loading.value = false }
}

function handleSearch() { pagination.current = 1; fetchList() }
function handleReset() {
  Object.assign(search, { keyword: '', apiGroup: '', apiName: '', apiPath: '' })
  activeModuleId.value = 0
  handleSearch()
}

// ===== 批量操作 =====
const selectedIds = computed(() => selectedRows.value.map((r: any) => r.id))
function handleSelectionChange(rows: any[]) { selectedRows.value = rows }
function clearSelection() { selectedRows.value = [] }
function handleBatchAction(key: string) {
  if (key === 'delete') handleBatchDelete()
  else if (key === 'modifyGroup') openBatchGroup()
}
function handleBatchDelete() {
  ElMessageBox.confirm(
    `确定删除选中的 ${selectedIds.value.length} 个关键字？`,
    '批量删除',
    { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' },
  ).then(async () => {
    for (const id of selectedIds.value) {
      await deleteKeyword(projectId.value, id)
    }
    ElMessage.success('删除成功')
    clearSelection()
    fetchList()
  }).catch(() => {})
}

function handleDelete(record: any) {
  ElMessageBox.confirm(`确定删除关键字「${record.name}」？`, '确认删除', { type: 'warning' })
    .then(async () => { await deleteKeyword(projectId.value, record.id); ElMessage.success('删除成功'); fetchList() })
    .catch(() => {})
}

// ===== 批量修改分组 =====
const batchGroupVisible = ref(false)
const batchGroupTarget = ref<number | null>(null)
const groupNameMap = computed<Record<number, string>>(() => {
  const map: Record<number, string> = {}
  modules.value.forEach((m: any) => { map[m.id] = m.name })
  return map
})
function openBatchGroup() {
  batchGroupTarget.value = null
  batchGroupVisible.value = true
}
async function applyBatchGroup() {
  if (!batchGroupTarget.value) { ElMessage.warning('请选择目标分组'); return }
  try {
    for (const kw of selectedRows.value) {
      await updateKeyword(projectId.value, kw.id, { ...kw, moduleId: batchGroupTarget.value })
    }
    ElMessage.success(`已成功将 ${selectedRows.value.length} 条接口关键字的分组修改为「${groupNameMap.value[batchGroupTarget.value]}」`)
    batchGroupVisible.value = false
    clearSelection()
    fetchList()
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '操作失败') }
}

// ===== 从接口生成 =====
const generateVisible = ref(false)
const generateLoading = ref(false)
const apis = ref<any[]>([])
const selectedApiId = ref<number>(0)
async function openGenerate() {
  try {
    const res: any = await getApis(projectId.value, { page: 1, pageSize: 100 })
    apis.value = res.data?.items || []
  } catch { apis.value = [] }
  selectedApiId.value = 0
  generateVisible.value = true
}
async function handleGenerate() {
  if (!selectedApiId.value) { ElMessage.warning('请选择接口'); return }
  generateLoading.value = true
  try {
    await generateKeyword(projectId.value, selectedApiId.value)
    ElMessage.success('生成成功')
    generateVisible.value = false
    fetchList()
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '生成失败') } finally { generateLoading.value = false }
}

// ===== 表字段调整 =====
const defaultColumns: ColumnItem[] = [
  { key: 'id', label: 'ID', locked: true, visible: true },
  { key: 'name', label: '接口关键字', locked: true, visible: true },
  { key: 'method', label: '方法', locked: false, visible: false },
  { key: 'apiGroup', label: '关联接口分组', locked: true, visible: true },
  { key: 'apiName', label: '关联接口名', locked: true, visible: true },
  { key: 'apiPath', label: '关联接口路径', locked: true, visible: true },
  { key: 'params', label: '参数', locked: false, visible: false },
  { key: 'response', label: '返回', locked: false, visible: false },
  { key: 'module', label: '分组', locked: false, visible: false },
  { key: 'category', label: '分类', locked: false, visible: false },
  { key: 'tags', label: '标签', locked: false, visible: false },
  { key: 'desc', label: '描述', locked: true, visible: true },
  { key: 'createTime', label: '创建时间', locked: false, visible: false },
  { key: 'refCount', label: '被引用次数', locked: false, visible: false },
  { key: 'action', label: '操作', locked: true, visible: true },
]
const columns = ref<ColumnItem[]>(defaultColumns.map((c) => ({ ...c })))
function isColVisible(key: string) {
  return columns.value.find((c) => c.key === key)?.visible ?? false
}
function resetColumns() {
  columns.value = defaultColumns.map((c) => ({ ...c }))
}

// ===== 格式化辅助 =====
function parseTags(raw?: string): string[] {
  if (!raw) return []
  try { const a = JSON.parse(raw); return Array.isArray(a) ? a : [] } catch { return [] }
}
function parseParamNames(raw?: string): string {
  if (!raw) return '--'
  try {
    const arr = JSON.parse(raw)
    if (!Array.isArray(arr)) return '--'
    return arr.map((p: any) => p.name).filter(Boolean).join(', ') || '--'
  } catch { return '--' }
}
function parseResponseFields(raw?: string): string {
  if (!raw) return '--'
  try {
    const obj = JSON.parse(raw)
    if (!obj?.fields) return '--'
    return obj.fields.map((f: any) => f.path).filter(Boolean).join(', ') || '--'
  } catch { return '--' }
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

// ===== 在线调试 =====
const debugVisible = ref(false)
const debugLoading = ref(false)
const debugRow = ref<any>(null)
const debugParams = ref<any[]>([])
const debugResult = ref<any>(null)
const debugEnvironments = ref<any[]>([])
const debugEnvId = ref<number | null>(null)

async function openDebug(row: any) {
  debugRow.value = row
  debugResult.value = null
  debugParams.value = []
  debugEnvId.value = null
  try {
    const data = JSON.parse(row.testData || '[]')
    debugParams.value = Array.isArray(data) ? data.map((p: any) => ({ ...p })) : []
  } catch { debugParams.value = [] }
  try {
    const res: any = await getEnvironments(projectId.value)
    debugEnvironments.value = res.data || []
  } catch { debugEnvironments.value = [] }
  debugVisible.value = true
}

async function executeDebug() {
  if (!debugRow.value) return
  if (!debugEnvId.value) {
    ElMessage.warning('请选择执行环境')
    return
  }
  debugLoading.value = true
  debugResult.value = null
  try {
    const res: any = await debugKeyword(projectId.value, debugRow.value.id, { environmentId: debugEnvId.value })
    const data = res.data || {}
    let responseBody: any = data.responseBody
    if (typeof responseBody === 'string') {
      try {
        responseBody = JSON.parse(responseBody)
      } catch {
        responseBody = { raw: data.responseBody }
      }
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

const debugAssertions = computed(() => {
  if (!debugRow.value || !debugResult.value) return []
  const assertions = parseAssertions(debugRow.value.responseAssertion)
  return assertions.map(a => {
    const actual = getNestedValue(debugResult.value.response, a.path)
    const expectedValue = a.expected || '非空即可'
    const actualValue = actual != null ? String(actual) : '--'
    const pass = expectedValue === '非空即可' ? actual != null : actualValue === expectedValue
    return { ...a, actual: actualValue, pass }
  })
})

onMounted(() => { fetchModules(); fetchList() })
</script>

<template>
  <div>
    <PageHeader title="接口关键字">
      <el-button v-if="hasPermission('project:keyword:from-api')" @click="openGenerate">从接口生成</el-button>
      <el-button v-if="hasPermission('project:keyword:add')" type="primary" @click="router.push(`/project/${projectId}/keywords/new`)">+ 新建接口关键字</el-button>
    </PageHeader>

    <div class="kw-layout">
      <!-- 左侧分组树（复用接口分组） -->
      <div class="module-panel">
        <div class="module-head">
          <span class="module-title">接口分组</span>
        </div>
        <div class="module-tree">
          <el-tree
            :data="moduleTree"
            node-key="id"
            :props="{ label: 'name', children: 'children' }"
            :default-expand-all="true"
            :expand-on-click-node="false"
            @node-click="onModuleNodeClick"
          >
            <template #default="{ data }">
              <div :class="['module-tree-node', { active: activeModuleId === data.id }]">
                <span class="module-name">{{ data.name }}</span>
                <span class="module-count">{{ data.apiCount ?? 0 }}</span>
              </div>
            </template>
          </el-tree>
        </div>
      </div>

      <!-- 右侧内容 -->
      <div class="kw-content">
        <ProSearchCard :loading="loading" @search="handleSearch" @reset="handleReset">
          <div class="pro-search-field">
            <span class="pro-search-label">接口关键字</span>
            <el-input v-model="search.keyword" placeholder="输入接口关键字" clearable style="width: 180px" @keyup.enter="handleSearch" />
          </div>
          <div class="pro-search-field">
            <span class="pro-search-label">关联接口分组</span>
            <el-select v-model="search.apiGroup" placeholder="全部分组" clearable style="width: 160px" @change="handleSearch">
              <el-option v-for="m in modules.filter((m: any) => m.isSystem !== 1)" :key="m.id" :value="m.name" :label="m.name" />
            </el-select>
          </div>
          <div class="pro-search-field">
            <span class="pro-search-label">关联接口名</span>
            <el-input v-model="search.apiName" placeholder="输入关联接口名" clearable style="width: 160px" @keyup.enter="handleSearch" />
          </div>
          <template #collapse>
            <div class="pro-search-field">
              <span class="pro-search-label">关联接口路径</span>
              <el-input v-model="search.apiPath" placeholder="输入关联接口路径" clearable style="width: 200px" @keyup.enter="handleSearch" />
            </div>
          </template>
        </ProSearchCard>

        <div class="table-toolbar">
          <ColumnSettings
            :columns="columns"
            @update:columns="(v: ColumnItem[]) => (columns = v)"
            @reset="resetColumns"
          />
        </div>

        <BatchBar
          :selected-count="selectedIds.length"
          :actions="[
            { key: 'modifyGroup', label: '批量修改分组' },
            { key: 'delete', label: '批量删除', danger: true },
          ]"
          @action="handleBatchAction"
          @clear="clearSelection"
        />

        <el-table :data="list" v-loading="loading" border stripe style="width: 100%" @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="45" />
          <el-table-column v-if="isColVisible('id')" prop="id" label="ID" width="70" show-overflow-tooltip />
          <el-table-column v-if="isColVisible('name')" prop="name" label="接口关键字" width="160" show-overflow-tooltip />
          <el-table-column v-if="isColVisible('method')" label="方法" width="80">
            <template #default="{ row }">
              <el-tag :type="methodColors[row.httpMethod] || 'info'" size="small">{{ row.httpMethod || '--' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column v-if="isColVisible('apiGroup')" label="关联接口分组" width="140" show-overflow-tooltip>
            <template #default="{ row }">{{ row.moduleName || moduleMap[row.moduleId]?.name || '--' }}</template>
          </el-table-column>
          <el-table-column v-if="isColVisible('apiName')" label="关联接口名" width="150" show-overflow-tooltip>
            <template #default="{ row }">{{ row.apiName || '--' }}</template>
          </el-table-column>
          <el-table-column v-if="isColVisible('apiPath')" label="关联接口路径" width="240" show-overflow-tooltip>
            <template #default="{ row }"><code style="font-size: 12px; color: #909399">{{ row.apiPath || '--' }}</code></template>
          </el-table-column>
          <el-table-column v-if="isColVisible('params')" label="参数" width="180" show-overflow-tooltip>
            <template #default="{ row }">{{ parseParamNames(row.testData) }}</template>
          </el-table-column>
          <el-table-column v-if="isColVisible('response')" label="返回" width="180" show-overflow-tooltip>
            <template #default="{ row }">{{ parseResponseFields(row.responseAssertion) }}</template>
          </el-table-column>
          <el-table-column v-if="isColVisible('module')" label="分组" width="120">
            <template #default="{ row }">{{ row.moduleName || moduleMap[row.moduleId]?.name || '--' }}</template>
          </el-table-column>
          <el-table-column v-if="isColVisible('category')" label="分类" width="120">
            <template #default="{ row }">
              <el-tag v-if="row.category" size="small">{{ row.category }}</el-tag>
              <span v-else style="color: #c0c4cc">--</span>
            </template>
          </el-table-column>
          <el-table-column v-if="isColVisible('tags')" label="标签" width="200">
            <template #default="{ row }">
              <el-tag v-for="t in parseTags(row.tags)" :key="t" size="small" type="info" style="margin-right: 4px">{{ t }}</el-tag>
              <span v-if="!parseTags(row.tags).length" style="color: #c0c4cc">--</span>
            </template>
          </el-table-column>
          <el-table-column v-if="isColVisible('desc')" prop="description" label="描述" show-overflow-tooltip />
          <el-table-column v-if="isColVisible('createTime')" label="创建时间" width="120">
            <template #default="{ row }">{{ row.createdAt?.substring(0, 10) }}</template>
          </el-table-column>
          <el-table-column v-if="isColVisible('refCount')" label="被引用次数" width="100">
            <template #default="{ row }">{{ row.referenceCount ?? 0 }}</template>
          </el-table-column>
          <el-table-column v-if="isColVisible('action')" label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button v-if="hasPermission('project:keyword:edit')" type="primary" link size="small" @click="router.push(`/project/${projectId}/keywords/${row.id}/edit`)">编辑</el-button>
              <el-button type="primary" link size="small" @click="openDebug(row)">调试</el-button>
              <el-button v-if="hasPermission('project:keyword:delete')" type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <ProPagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          @change="(p: number) => { pagination.current = p; fetchList() }"
        />
      </div>
    </div>

    <!-- 从接口生成弹窗 -->
    <el-dialog v-model="generateVisible" title="从接口快速生成关键字" width="500px">
      <el-form-item label="选择接口">
        <el-select v-model="selectedApiId" placeholder="选择要生成关键字的接口" filterable style="width: 100%">
          <el-option v-for="api in apis" :key="api.id" :value="api.id" :label="`[${api.httpMethod}] ${api.name}`" />
        </el-select>
      </el-form-item>
      <template #footer>
        <el-button @click="generateVisible = false">取消</el-button>
        <el-button type="primary" :loading="generateLoading" @click="handleGenerate">生成</el-button>
      </template>
    </el-dialog>

    <!-- 批量修改分组弹窗 -->
    <el-dialog v-model="batchGroupVisible" title="批量修改分组" width="420px">
      <p style="font-size: 13px; color: #606266; margin-bottom: 16px">
        将已选中的 <b style="color: #409eff">{{ selectedRows.length }}</b> 条接口关键字的分组修改为：
      </p>
      <el-form-item label="目标分组" required>
        <el-select v-model="batchGroupTarget" placeholder="请选择分组" filterable style="width: 100%">
          <el-option v-for="m in modules.filter((m: any) => m.isSystem !== 1)" :key="m.id" :value="m.id" :label="m.name" />
        </el-select>
      </el-form-item>
      <div v-if="selectedRows.length" style="margin-top: 12px; padding: 12px; background: #f5f7fa; border-radius: 4px; font-size: 13px; max-height: 120px; overflow-y: auto">
        <div style="color: #909399; margin-bottom: 6px">以下接口关键字的分组将被修改：</div>
        <div v-for="row in selectedRows" :key="row.id" style="padding: 2px 0">
          <span style="color: #606266">{{ row.name }}</span>
          <span style="margin: 0 6px; color: #c0c4cc; font-size: 12px">{{ row.moduleName || '未分组' }}</span>
          <span style="color: #c0c4cc">→</span>
          <span style="color: #409eff; font-weight: 500; margin-left: 4px">{{ batchGroupTarget ? groupNameMap[batchGroupTarget] : '请选择' }}</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="batchGroupVisible = false">取消</el-button>
        <el-button type="primary" @click="applyBatchGroup">确认修改</el-button>
      </template>
    </el-dialog>

    <!-- 在线调试弹窗 -->
    <el-dialog v-model="debugVisible" :title="`在线调试${debugRow ? '：' + debugRow.name : ''}`" width="680px">
      <!-- 关键字信息摘要 -->
      <div v-if="debugRow" style="display: flex; align-items: center; gap: 16px; margin-bottom: 12px; font-size: 13px">
        <div><span style="color: #909399">ID：</span><code style="font-family: monospace; font-size: 13px; color: #303133">{{ debugRow.id }}</code></div>
        <div><span style="color: #909399">关联接口分组：</span><el-tag size="small" type="info">{{ debugRow.moduleName || '--' }}</el-tag></div>
      </div>
      <!-- 接口信息栏 -->
      <div v-if="debugRow" class="debug-api-bar">
        <el-tag :type="methodColors[debugRow.httpMethod] || 'info'" size="small">{{ debugRow.httpMethod || '--' }}</el-tag>
        <code style="font-size: 13px">{{ debugRow.apiPath || '--' }}</code>
        <span style="color: #909399; margin-left: auto">{{ debugRow.description }}</span>
      </div>
      <!-- 环境选择 + 执行按钮 -->
      <div class="debug-env-row">
        <span style="font-size: 13px; color: #606266">执行环境：</span>
        <el-select v-model="debugEnvId" placeholder="选择环境" style="width: 160px" size="small">
          <el-option v-for="env in debugEnvironments" :key="env.id" :value="env.id" :label="env.name" />
        </el-select>
        <el-button type="primary" :loading="debugLoading" @click="executeDebug" style="margin-left: auto">发送请求</el-button>
      </div>
      <!-- 请求参数 -->
      <div class="debug-section-title">请求参数</div>
      <el-table :data="debugParams" size="small" border style="margin-bottom: 16px">
        <el-table-column label="参数名" width="140">
          <template #default="{ row }"><code>{{ row.name }}</code></template>
        </el-table-column>
        <el-table-column label="类型" width="80">
          <template #default="{ row }"><el-tag size="small">{{ row.type || 'string' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="测试值">
          <template #default="{ row }"><el-input v-model="row.value" size="small" placeholder="测试值" /></template>
        </el-table-column>
      </el-table>
      <!-- 响应结果 -->
      <div v-if="debugResult" class="debug-result-section">
        <div :class="['debug-result-header', debugResult.success ? 'success' : 'fail']">
          <span style="font-weight: 600">{{ debugResult.success ? '✓' : '✗' }} {{ debugResult.statusCode }}</span>
          <span>{{ debugResult.success ? '请求成功' : '请求失败' }}</span>
          <span style="margin-left: auto; color: #909399">耗时 {{ debugResult.duration }}ms</span>
        </div>
        <div class="debug-section-title" style="margin-top: 10px">响应体</div>
        <pre class="debug-response-box">{{ JSON.stringify(debugResult.response, null, 2) }}</pre>
        <!-- 断言结果 -->
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
                  {{ row.pass ? '✓ 通过' : '✗ 失败' }}
                </span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
      <template #footer>
        <el-button @click="debugVisible = false">关闭</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<style scoped>
.kw-layout {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}
.module-panel {
  width: 220px;
  flex-shrink: 0;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 12px;
}
.module-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.module-title {
  font-weight: 600;
  font-size: 14px;
  color: #303133;
}
.module-tree {
  max-height: 560px;
  overflow-y: auto;
  margin-top: 8px;
}
.module-tree :deep(.el-tree-node__content) {
  height: auto;
  padding: 2px 0;
}
.module-tree-node {
  display: flex;
  align-items: center;
  flex: 1;
  padding: 2px 4px;
  border-radius: 4px;
  font-size: 13px;
  gap: 6px;
  width: 100%;
}
.module-tree-node:hover {
  background: #f5f7fa;
}
.module-tree-node.active {
  background: #ecf5ff;
  color: #409eff;
  font-weight: 500;
}
.module-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.module-count {
  font-size: 12px;
  color: #909399;
  flex-shrink: 0;
}
.kw-content {
  flex: 1;
  min-width: 0;
}
.table-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 8px;
}
/* 在线调试弹窗样式 */
.debug-api-bar {
  padding: 10px 12px;
  background: #fafafa;
  border-radius: 4px;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  margin-bottom: 16px;
}
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
.debug-assertions {
  margin-top: 10px;
}
</style>
