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
import { ref, reactive, onMounted, onUnmounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getKeywords, deleteKeyword, updateKeyword, debugKeyword,
  getKeywordGroups, createKeywordGroup, updateKeywordGroup, deleteKeywordGroup,
  clearKeywordGroupKeywords, clearAllKeywords,
} from '@/api/keyword'
import { getEnvironments } from '@/api/environment'
import CodeEditor from '@/components/CodeEditor/index.vue'
import { formatJson } from '@/utils/jsonFormat'
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

// 列表中仅显示 path，去除前导 ${host} 等环境变量占位符
function displayApiPath(path: string | undefined): string {
  return (path || '').replace(/^\$\{[^}]*\}/, '')
}

// 文本截断，超过指定长度显示省略号
function truncateText(text: string | undefined, maxLen: number): string {
  const s = text || ''
  return s.length > maxLen ? s.slice(0, maxLen) + '...' : s
}

// 去除 JSON 字符串中的行内注释，避免后端解析失败
function stripJsonComments(json: string): string {
  return json.replace(/\/\/.*$/gm, '')
}

// ===== 列表数据 =====
const loading = ref(false)
const list = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const selectedRows = ref<any[]>([])

// ===== 搜索条件 =====
const search = reactive({ keyword: '', apiPath: '' })

// ===== 分组树（接口关键字独立分组） =====
const groups = ref<any[]>([])
const activeGroupId = ref<number>(0) // 0 = 全部
const filterText = ref('')
const groupMap = computed<Record<number, any>>(() => {
  const m: Record<number, any> = {}
  groups.value.forEach((g) => { m[g.id] = g })
  return m
})
// 批量移动可选分组（用户分组 + 未分组系统分组）
const moveTargetGroups = computed(() =>
  groups.value.filter((g) => g.isSystem !== 1 || g.name === '未分组'),
)
// 分组树：全部(虚拟) + 系统分组(未分组等，排除全部) + 用户分组按 parentId 建树
const groupTree = computed(() => {
  const userGroups = groups.value.filter((g) => g.isSystem !== 1)
  const buildTree = (parentId: number | null): any[] =>
    userGroups
      .filter((g) => (g.parentId ?? null) === parentId)
      .map((g) => ({ ...g, keywordCount: g.keywordCount ?? 0, children: buildTree(g.id) }))
  const systemGroups = groups.value
    .filter((g) => g.isSystem === 1 && g.name !== '全部')
    .map((g) => ({ ...g, keywordCount: g.keywordCount ?? 0, children: [] }))
  return [
    { id: 0, name: '全部', isSystem: 1, keywordCount: pagination.total, children: [] },
    ...systemGroups,
    ...buildTree(null),
  ]
})
// 根据搜索关键字过滤分组树
const filteredGroupTree = computed(() => {
  const kw = filterText.value.trim().toLowerCase()
  if (!kw) return groupTree.value
  const matchRecursive = (nodes: any[]): any[] => {
    const result: any[] = []
    for (const node of nodes) {
      const childMatches = matchRecursive(node.children || [])
      if (node.name.toLowerCase().includes(kw) || childMatches.length > 0) {
        result.push({ ...node, children: childMatches.length > 0 ? childMatches : node.children })
      }
    }
    return result
  }
  return matchRecursive(groupTree.value)
})
function onGroupNodeClick(data: any) {
  activeGroupId.value = data.id
  pagination.current = 1
  fetchList()
}

async function fetchGroups() {
  try {
    const res: any = await getKeywordGroups(projectId.value)
    groups.value = res.data || []
  } catch { groups.value = [] }
}

async function fetchList() {
  loading.value = true
  try {
    const res: any = await getKeywords(projectId.value, {
      groupId: activeGroupId.value || undefined,
      keyword: search.keyword || undefined,
      apiPath: search.apiPath || undefined,
      page: pagination.current, pageSize: pagination.pageSize,
    })
    list.value = res.data?.items || []
    pagination.total = res.data?.total || 0
  } catch { list.value = [] } finally { loading.value = false }
}

function handleSearch() { pagination.current = 1; fetchList() }
function handleReset() {
  Object.assign(search, { keyword: '', apiPath: '' })
  activeGroupId.value = 0
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
  groups.value.forEach((g: any) => { map[g.id] = g.name })
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
      await updateKeyword(projectId.value, kw.id, { ...kw, groupId: batchGroupTarget.value })
    }
    ElMessage.success(`已成功将 ${selectedRows.value.length} 条接口关键字的分组修改为「${groupNameMap.value[batchGroupTarget.value]}」`)
    batchGroupVisible.value = false
    batchGroupTarget.value = null
    clearSelection()
    fetchGroups()
    fetchList()
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '操作失败') }
}

// ===== 分组管理 =====
const manageGroupsVisible = ref(false)
const editingGroupId = ref<number>(0)
const groupForm = reactive({ name: '', parentId: null as number | null, description: '' })

function openCreateGroup() {
  editingGroupId.value = 0
  groupForm.name = ''
  groupForm.parentId = null
  groupForm.description = ''
}
function openEditGroup(g: any) {
  if (g.isSystem === 1) {
    ElMessage.warning('系统默认分组不可编辑')
    return
  }
  editingGroupId.value = g.id
  groupForm.name = g.name
  groupForm.parentId = g.parentId ?? null
  groupForm.description = g.description || ''
}
async function handleGroupSubmit() {
  if (!groupForm.name.trim()) { ElMessage.warning('请输入分组名称'); return }
  const payload = { name: groupForm.name.trim(), parentId: groupForm.parentId, description: groupForm.description?.trim() || undefined }
  try {
    if (editingGroupId.value) {
      await updateKeywordGroup(projectId.value, editingGroupId.value, payload)
      ElMessage.success('更新成功')
    } else {
      await createKeywordGroup(projectId.value, payload)
      ElMessage.success('创建成功')
    }
    groupForm.name = ''
    groupForm.parentId = null
    groupForm.description = ''
    editingGroupId.value = 0
    manageGroupsVisible.value = false
    fetchGroups()
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '操作失败') }
}
function handleDeleteGroup(g: any) {
  if (g.isSystem === 1) {
    ElMessage.warning('系统默认分组不可删除')
    return
  }
  ElMessageBox.confirm(`确定删除分组「${g.name}」？该分组下的关键字将变为未分组。`, '确认删除', { type: 'warning' })
    .then(async () => {
      await deleteKeywordGroup(projectId.value, g.id)
      ElMessage.success('删除成功')
      fetchGroups()
      fetchList()
    })
    .catch(() => {})
}

// ===== 右键上下文菜单 =====
const contextMenuVisible = ref(false)
const contextMenuPos = reactive({ x: 0, y: 0 })
const contextGroup = ref<any>(null)

function closeContextMenu() {
  contextMenuVisible.value = false
  contextGroup.value = null
}
function onPanelContextMenu(e: MouseEvent) {
  e.preventDefault()
  contextGroup.value = null
  contextMenuPos.x = e.clientX
  contextMenuPos.y = e.clientY
  contextMenuVisible.value = true
}
function onNodeContextMenu(e: MouseEvent, data: any) {
  e.preventDefault()
  e.stopPropagation()
  // 系统分组仅允许"全部"(id=0)和"未分组"显示清空菜单
  if (data.isSystem === 1 && data.id !== 0 && data.name !== '未分组') return
  contextGroup.value = data
  contextMenuPos.x = e.clientX
  contextMenuPos.y = e.clientY
  contextMenuVisible.value = true
}
function contextCreateGroup() {
  closeContextMenu()
  openCreateGroup()
  manageGroupsVisible.value = true
}
function contextCreateChild() {
  if (!contextGroup.value) return
  editingGroupId.value = 0
  groupForm.name = ''
  groupForm.parentId = contextGroup.value.id
  groupForm.description = ''
  manageGroupsVisible.value = true
  closeContextMenu()
}
function contextEdit() {
  if (!contextGroup.value) return
  openEditGroup(contextGroup.value)
  manageGroupsVisible.value = true
  closeContextMenu()
}
function contextDelete() {
  if (!contextGroup.value) return
  handleDeleteGroup(contextGroup.value)
  closeContextMenu()
}
function contextClear() {
  if (!contextGroup.value) return
  const g = contextGroup.value
  closeContextMenu()
  const isAll = g.id === 0
  ElMessageBox.confirm(
    isAll
      ? '确定清空项目下的所有关键字？此操作不可恢复。'
      : `确定清空分组「${g.name}」及其子分组中的所有关键字？此操作不可恢复。`,
    '确认清空',
    { type: 'warning', confirmButtonText: '清空', cancelButtonText: '取消' },
  )
    .then(async () => {
      if (isAll) {
        await clearAllKeywords(projectId.value)
      } else {
        await clearKeywordGroupKeywords(projectId.value, g.id)
      }
      ElMessage.success('已清空')
      fetchGroups()
      fetchList()
    })
    .catch(() => {})
}

onMounted(() => {
  document.addEventListener('click', closeContextMenu)
  fetchGroups()
  fetchList()
})
onUnmounted(() => {
  document.removeEventListener('click', closeContextMenu)
})

// ===== 表字段调整 =====
const defaultColumns: ColumnItem[] = [
  { key: 'id', label: 'ID', locked: true, visible: true },
  { key: 'name', label: '接口关键字', locked: true, visible: true },
  { key: 'method', label: '方法', locked: false, visible: false },
  { key: 'apiGroup', label: '关联接口分组', locked: true, visible: true },
  { key: 'apiPath', label: '关联接口路径', locked: true, visible: true },
  { key: 'group', label: '分组', locked: false, visible: true },
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

// ===== 调试参数分区（结构与编辑页“请求参数”对齐；无 in 字段的旧数据归入 Query 区兑底） =====
const debugPathParams = computed(() => debugParams.value.filter(p => p.in === 'path'))
const debugQueryParams = computed(() => debugParams.value.filter(p => p.in !== 'path' && p.in !== 'body'))
const debugBodyRow = computed(() => debugParams.value.find(p => p.in === 'body' && p.name === '__body__'))
const debugBodyKvParams = computed(() => debugParams.value.filter(p => p.in === 'body' && p.name !== '__body__'))
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
    const testData = debugParams.value.map((p: any) => {
      if (p.in === 'body' && p.name === '__body__' && typeof p.value === 'string') {
        return { ...p, value: stripJsonComments(p.value) }
      }
      return p
    })
    const res: any = await debugKeyword(projectId.value, debugRow.value.id, {
      environmentId: debugEnvId.value,
      testData: JSON.stringify(testData),
    })
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

// ===== 调试结果：环境名称 =====
const debugEnvName = computed(() => {
  if (!debugEnvId.value) return ''
  const env = debugEnvironments.value.find((e: any) => e.id === debugEnvId.value)
  return env?.name || ''
})

// ===== 调试结果：JSON 语法高亮 =====
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
</script>

<template>
  <div>
    <PageHeader title="接口关键字">
      <el-button v-if="hasPermission('project:keyword:add')" type="primary" @click="router.push(`/project/${projectId}/keywords/new`)">+ 新建接口关键字</el-button>
    </PageHeader>

    <div class="kw-layout">
      <!-- 左侧分组树（接口关键字独立分组） -->
      <div class="module-panel" @contextmenu="onPanelContextMenu">
        <div class="module-head">
          <span class="module-title">分组</span>
        </div>
        <el-input
          v-model="filterText"
          class="tree-search"
          placeholder="搜索分组"
          clearable
          size="small"
          prefix-icon="Search"
        />
        <div class="module-tree">
          <el-tree
            :data="filteredGroupTree"
            node-key="id"
            :props="{ label: 'name', children: 'children' }"
            :default-expand-all="true"
            :expand-on-click-node="false"
            :indent="0"
            @node-click="onGroupNodeClick"
          >
            <template #default="{ data }">
              <div
                :class="['module-tree-node', { active: activeGroupId === data.id }]"
                @contextmenu.stop="onNodeContextMenu($event, data)"
              >
                <span class="module-name">{{ data.name }}</span>
                <span v-if="data.isSystem === 1" class="module-lock" title="系统默认分组">🔒</span>
                <span class="module-count">{{ data.keywordCount ?? 0 }}</span>
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
            <span class="pro-search-label">关联接口路径</span>
            <el-input v-model="search.apiPath" placeholder="输入关联接口路径" clearable style="width: 200px" @keyup.enter="handleSearch" />
          </div>
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
          <el-table-column v-if="isColVisible('name')" label="接口关键字" min-width="420">
            <template #default="{ row }"><span :title="row.name">{{ truncateText(row.name, 50) || '--' }}</span></template>
          </el-table-column>
          <el-table-column v-if="isColVisible('method')" label="方法" width="80">
            <template #default="{ row }">
              <el-tag :type="methodColors[row.httpMethod] || 'info'" size="small">{{ row.httpMethod || '--' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column v-if="isColVisible('apiGroup')" label="关联接口分组" width="140" show-overflow-tooltip>
            <template #default="{ row }">{{ row.moduleName || '--' }}</template>
          </el-table-column>
          <el-table-column v-if="isColVisible('apiPath')" label="关联接口路径" width="240" show-overflow-tooltip>
            <template #default="{ row }"><code style="font-size: 12px; color: #909399">{{ displayApiPath(row.apiPath) || '--' }}</code></template>
          </el-table-column>
          <el-table-column v-if="isColVisible('group')" label="分组" width="140">
            <template #default="{ row }">{{ row.groupName || groupMap[row.groupId]?.name || '--' }}</template>
          </el-table-column>
          <el-table-column v-if="isColVisible('desc')" label="描述" show-overflow-tooltip>
            <template #default="{ row }"><span :title="row.description">{{ truncateText(row.description, 10) || '--' }}</span></template>
          </el-table-column>
          <el-table-column v-if="isColVisible('createTime')" label="创建时间" width="120">
            <template #default="{ row }">{{ row.createdAt?.substring(0, 10) }}</template>
          </el-table-column>
          <el-table-column v-if="isColVisible('refCount')" label="被引用次数" width="100">
            <template #default="{ row }">{{ row.referenceCount ?? 0 }}</template>
          </el-table-column>
          <el-table-column v-if="isColVisible('action')" label="操作" width="140" fixed="right">
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

    <!-- 批量修改分组弹窗 -->
    <el-dialog v-model="batchGroupVisible" title="批量修改分组" width="420px">
      <p style="font-size: 13px; color: #606266; margin-bottom: 16px">
        将已选中的 <b style="color: #409eff">{{ selectedRows.length }}</b> 条接口关键字的分组修改为：
      </p>
      <el-form-item label="目标分组" required>
        <el-select v-model="batchGroupTarget" placeholder="请选择分组" filterable style="width: 100%">
          <el-option v-for="g in moveTargetGroups" :key="g.id" :value="g.id" :label="g.name" />
        </el-select>
      </el-form-item>
      <div v-if="selectedRows.length" style="margin-top: 12px; padding: 12px; background: #f5f7fa; border-radius: 4px; font-size: 13px; max-height: 120px; overflow-y: auto">
        <div style="color: #909399; margin-bottom: 6px">以下接口关键字的分组将被修改：</div>
        <div v-for="row in selectedRows" :key="row.id" style="padding: 2px 0">
          <span style="color: #606266">{{ row.name }}</span>
          <span style="margin: 0 6px; color: #c0c4cc; font-size: 12px">{{ row.groupName || '未分组' }}</span>
          <span style="color: #c0c4cc">→</span>
          <span style="color: #409eff; font-weight: 500; margin-left: 4px">{{ batchGroupTarget ? groupNameMap[batchGroupTarget] : '请选择' }}</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="batchGroupVisible = false">取消</el-button>
        <el-button type="primary" @click="applyBatchGroup">确认修改</el-button>
      </template>
    </el-dialog>

    <!-- 分组新建/编辑弹窗 -->
    <el-dialog v-model="manageGroupsVisible" :title="editingGroupId ? '编辑分组' : '新建分组'" width="460px">
      <el-form label-position="top">
        <el-form-item label="分组名称" required>
          <el-input v-model="groupForm.name" placeholder="如：用户管理服务" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="groupForm.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="manageGroupsVisible = false">取消</el-button>
        <el-button type="primary" @click="handleGroupSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 在线调试弹窗 -->
    <el-dialog v-model="debugVisible" title="在线调试" width="680px">
      <!-- 关键字信息摘要 -->
      <div v-if="debugRow" style="display: flex; flex-direction: column; gap: 10px; margin-bottom: 12px; font-size: 13px">
        <div><span style="color: #909399">ID：</span><code style="font-family: monospace; font-size: 13px; color: #303133">{{ debugRow.id }}</code></div>
        <div><span style="color: #909399">接口关键字名称：</span><span style="font-weight: 500; color: #303133">{{ debugRow.name || '--' }}</span></div>
        <div><span style="color: #909399">关联接口分组：</span><el-tag size="small" type="info">{{ debugRow.moduleName || '--' }}</el-tag></div>
        <div style="display: flex; align-items: center; gap: 10px; flex-wrap: wrap">
          <span style="color: #909399">关联接口：</span>
          <el-tag :type="methodColors[debugRow.httpMethod] || 'info'" size="small">{{ debugRow.httpMethod || '--' }}</el-tag>
          <code style="font-size: 13px">{{ debugRow.apiPath || '--' }}</code>
          <span style="color: #909399; margin-left: auto">{{ debugRow.description }}</span>
        </div>
      </div>
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
      <!-- 请求参数（分区结构对齐编辑页：Path / Query / 请求体，仅值可编辑） -->
      <div class="debug-section-title">请求参数</div>
      <div v-if="debugParams.length" style="margin-bottom: 16px">
        <!-- Path 参数 -->
        <div v-if="debugPathParams.length" class="debug-param-section">
          <h4>Path 参数</h4>
          <el-table :data="debugPathParams" size="small" border>
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
        <div v-if="debugQueryParams.length" class="debug-param-section">
          <h4>Query 参数</h4>
          <el-table :data="debugQueryParams" size="small" border>
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
        <div v-if="debugBodyRow || debugBodyKvParams.length" class="debug-param-section">
          <h4>请求体</h4>
          <template v-if="debugBodyRow">
            <div v-if="debugBodyRow.type === 'json'" style="display: flex; justify-content: flex-end; margin-bottom: 8px">
              <el-button size="small" @click="debugBodyRow.value = formatJson(debugBodyRow.value)">格式化</el-button>
            </div>
            <div v-if="debugBodyRow.type === 'json'" style="height: 200px">
              <CodeEditor v-model="debugBodyRow.value" language="json" :min-height="160" placeholder='如 {"key": "value"}' />
            </div>
            <el-input v-else v-model="debugBodyRow.value" type="textarea" :rows="6" placeholder='如 {"key": "value"}' style="font-family: monospace" />
          </template>
          <el-table v-else :data="debugBodyKvParams" size="small" border>
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
        <!-- 加载中状态 -->
        <div v-if="debugLoading && !debugResult" class="debug-result-header loading">
          <span class="debug-spin-icon">&#8635;</span>
          <span>正在发送请求...</span>
        </div>
        <!-- 结果展示 -->
        <template v-else-if="debugResult">
          <div :class="['debug-result-header', debugResult.success ? 'success' : 'fail']">
            <span style="font-weight: 600">{{ debugResult.success ? '&#10003;' : '&#10007;' }} {{ debugResult.statusCode }}</span>
            <span>{{ debugResult.success ? '请求成功' : '请求失败' }}</span>
            <span style="margin-left: auto; color: #909399">耗时 {{ debugResult.duration }}ms</span>
            <span style="color: #909399">环境：{{ debugEnvName }}</span>
          </div>
          <div class="debug-section-title" style="margin-top: 10px">响应体</div>
          <pre class="debug-response-box" v-html="highlightedDebugResponse"></pre>
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
                    {{ row.pass ? '&#10003; 通过' : '&#10007; 失败' }}
                  </span>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </template>
      </div>
      <template #footer>
        <el-button @click="debugVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 右键上下文菜单 -->
    <Teleport to="body">
      <div
        v-if="contextMenuVisible"
        class="context-menu"
        :style="{ left: contextMenuPos.x + 'px', top: contextMenuPos.y + 'px' }"
        @click.stop
      >
        <!-- 空白区域右键：仅显示"新建分组" -->
        <template v-if="!contextGroup">
          <div class="context-menu-item" @click="contextCreateGroup">新建分组</div>
        </template>
        <!-- 系统分组右键：仅允许清空 -->
        <template v-else-if="contextGroup.isSystem === 1">
          <div class="context-menu-item danger" @click="contextClear">清空关键字</div>
        </template>
        <!-- 用户分组右键 -->
        <template v-else>
          <div class="context-menu-item" @click="contextCreateChild">新建子分组</div>
          <div class="context-menu-divider" />
          <div class="context-menu-item" @click="contextEdit">编辑</div>
          <div class="context-menu-item danger" @click="contextClear">清空关键字</div>
          <div class="context-menu-item danger" @click="contextDelete">删除</div>
        </template>
      </div>
    </Teleport>

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
.tree-search {
  margin: 8px 0;
}
.tree-search :deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px #dcdfe6 inset;
  border-radius: 4px;
}
.module-tree {
  max-height: 560px;
  overflow-y: auto;
  margin: 8px -12px 0;
}
.module-tree :deep(.el-tree-node__content) {
  height: auto;
  padding: 0;
  width: 100%;
}
.module-tree-node {
  display: flex;
  align-items: center;
  flex: 1;
  padding: 4px 12px;
  border-radius: 0;
  font-size: 13px;
  gap: 6px;
  width: 100%;
  box-sizing: border-box;
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
.module-lock {
  font-size: 10px;
  color: #c0c4cc;
  flex-shrink: 0;
  margin-left: 2px;
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

/* 右键上下文菜单 */
.context-menu {
  position: fixed;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  padding: 4px 0;
  min-width: 130px;
  z-index: 9999;
}
.context-menu-item {
  padding: 7px 14px;
  font-size: 13px;
  color: #303133;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: background 0.15s;
}
.context-menu-item:hover {
  background: #f5f7fa;
}
.context-menu-item.danger {
  color: #f56c6c;
}
.context-menu-item.danger:hover {
  background: #fef0f0;
}
.context-menu-item.disabled {
  color: #c0c4cc;
  cursor: not-allowed;
}
.context-menu-divider {
  height: 1px;
  background: #ebeef5;
  margin: 4px 0;
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
/* 调试弹窗请求参数分区（对齐编辑页样式） */
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
/* 调试加载状态 */
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
/* JSON 语法高亮 */
.debug-response-box :deep(.json-key) { color: #89b4fa; }
.debug-response-box :deep(.json-str) { color: #a6e3a1; }
.debug-response-box :deep(.json-num) { color: #fab387; }
.debug-response-box :deep(.json-bool) { color: #cba6f7; }
.debug-response-box :deep(.json-null) { color: #9399b2; }
</style>
