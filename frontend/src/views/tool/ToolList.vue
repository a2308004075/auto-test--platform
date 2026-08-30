<!--
 @author HXN
 @date 2026-08-23
 @description 工具方法列表视图（对齐原型 tool-list.html）
-->
<script setup lang="ts">
/**
 * 工具方法列表 - M6
 * 左侧分组树（按 category 动态构建）+ 右侧高级搜索 + 批量操作 + 表字段调整 + 智能分页
 * 对齐原型 tool-list.html
 */
import { ref, reactive, onMounted, onBeforeUnmount, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTools, deleteTool, testTool, updateTool } from '@/api/tool'
import { usePermission } from '@/composables/usePermission'
import PageHeader from '@/components/PageHeader/index.vue'
import ProSearchCard from '@/components/ProSearchCard/index.vue'
import BatchBar from '@/components/BatchBar/index.vue'
import ColumnSettings, { type ColumnItem } from '@/components/ColumnSettings/index.vue'
import ProPagination from '@/components/ProPagination/index.vue'

const route = useRoute()
const router = useRouter()
const { hasPermission } = usePermission()
const projectId = computed(() => Number(route.params.id))

// ===== 右键菜单 =====
const contextMenuVisible = ref(false)
const contextMenuPos = reactive({ x: 0, y: 0 })
const contextNode = ref<any>(null)

function handleNodeContextmenu(e: MouseEvent, node: any) {
  e.preventDefault()
  e.stopPropagation()
  contextNode.value = node
  contextMenuPos.x = e.clientX
  contextMenuPos.y = e.clientY
  contextMenuVisible.value = true
}

function handleBlankContextmenu(e: MouseEvent) {
  e.preventDefault()
  contextNode.value = null
  contextMenuPos.x = e.clientX
  contextMenuPos.y = e.clientY
  contextMenuVisible.value = true
}

function closeContextMenu() {
  contextMenuVisible.value = false
  contextNode.value = null
}

function contextClear() {
  if (!contextNode.value) return
  const node = contextNode.value
  closeContextMenu()
  const isAll = node.id === 'ALL'
  ElMessageBox.confirm(
    isAll
      ? '确定清空项目下的所有工具方法？此操作不可恢复。'
      : `确定清空「${node.name}」分类下的所有工具方法？此操作不可恢复。`,
    '确认清空',
    { type: 'warning', confirmButtonText: '清空', cancelButtonText: '取消' },
  )
    .then(async () => {
      let targets: any[]
      if (isAll) {
        targets = allTools.value
      } else if (node.id === 'UNGROUPED') {
        targets = allTools.value.filter((t) => !t.category || t.category === 'CUSTOM' || t.category === 'BUILTIN')
      } else {
        targets = allTools.value.filter((t) => t.category === node.id)
      }
      for (const tool of targets) {
        await deleteTool(projectId.value, tool.id)
      }
      ElMessage.success('已清空')
      fetchAllTools()
    })
    .catch(() => {})
}

function contextCreateGroup() {
  closeContextMenu()
  ElMessageBox.prompt('请输入分组名称', '新建分组', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputPattern: /\S+/,
    inputErrorMessage: '分组名称不能为空',
  }).then(async ({ value }) => {
    const name = value.trim()
    const ungrouped = allTools.value.filter((t) => !t.category || t.category === 'CUSTOM' || t.category === 'BUILTIN')
    if (ungrouped.length === 0) {
      ElMessage.info('当前没有未分组的工具方法，分组会在工具方法被分配到该分组时自动创建')
      return
    }
    for (const tool of ungrouped) {
      await updateTool(projectId.value, tool.id, { ...tool, category: name })
    }
    ElMessage.success('已创建分组「' + name + '」')
    fetchAllTools()
  }).catch(() => {})
}

function contextEdit() {
  if (!contextNode.value) return
  const node = contextNode.value
  closeContextMenu()
  ElMessageBox.prompt('请输入新的分组名称', '编辑分组', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputValue: node.name,
    inputPattern: /\S+/,
    inputErrorMessage: '分组名称不能为空',
  }).then(async ({ value }) => {
    const newName = value.trim()
    if (newName === node.name) return
    const tools = allTools.value.filter((t) => t.category === node.id)
    for (const tool of tools) {
      await updateTool(projectId.value, tool.id, { ...tool, category: newName })
    }
    ElMessage.success('已重命名为「' + newName + '」')
    fetchAllTools()
  }).catch(() => {})
}

function contextDelete() {
  if (!contextNode.value) return
  const node = contextNode.value
  closeContextMenu()
  ElMessageBox.confirm(
    `确定删除分组「${node.name}」？该分组下的工具方法将被删除且不可恢复。`,
    '确认删除',
    { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
  ).then(async () => {
    const targets = allTools.value.filter((t) => t.category === node.id)
    for (const tool of targets) {
      await deleteTool(projectId.value, tool.id)
    }
    ElMessage.success('已删除')
    fetchAllTools()
  }).catch(() => {})
}

// ===== 全量数据（用于构建分组树和客户端筛选） =====
const allTools = ref<any[]>([])
const loading = ref(false)
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const selectedRows = ref<any[]>([])

// ===== 搜索条件 =====
const search = reactive({ keyword: '', description: '' })

// ===== 分组树（按 category 动态构建） =====
const activeCategory = ref('ALL')
const groupSearch = ref('')

const categoryTree = computed(() => {
  const categories = new Map<string, number>()
  let ungroupedCount = 0
  allTools.value.forEach((t) => {
    const cat = t.category
    if (!cat || cat === 'CUSTOM' || cat === 'BUILTIN') {
      ungroupedCount++
    } else {
      categories.set(cat, (categories.get(cat) || 0) + 1)
    }
  })
  const systemNodes = [
    { id: 'ALL', name: '全部', count: allTools.value.length, isSystem: true },
    { id: 'UNGROUPED', name: '未分组', count: ungroupedCount, isSystem: true },
  ]
  const userNodes = Array.from(categories.entries()).map(([name, count]) => ({
    id: name, name, count, isSystem: false,
  }))
  return [...systemNodes, ...userNodes]
})

const filteredCategoryTree = computed(() => {
  const kw = groupSearch.value.trim().toLowerCase()
  if (!kw) return categoryTree.value
  return categoryTree.value.filter((node) => {
    if (node.isSystem) return true
    return node.name.toLowerCase().includes(kw)
  })
})

function onCategoryClick(node: any) {
  activeCategory.value = node.id
  pagination.current = 1
}

// ===== 客户端筛选 + 分页 =====
const filteredList = computed(() => {
  let result = allTools.value
  if (activeCategory.value === 'UNGROUPED') {
    result = result.filter((t) => !t.category || t.category === 'CUSTOM' || t.category === 'BUILTIN')
  } else if (activeCategory.value !== 'ALL') {
    result = result.filter((t) => t.category === activeCategory.value)
  }
  const kw = search.keyword.trim().toLowerCase()
  const desc = search.description.trim().toLowerCase()
  if (kw) result = result.filter((t) => t.name?.toLowerCase().includes(kw))
  if (desc) result = result.filter((t) => t.description?.toLowerCase().includes(desc))
  return result
})

const pagedList = computed(() => {
  const start = (pagination.current - 1) * pagination.pageSize
  return filteredList.value.slice(start, start + pagination.pageSize)
})

watch(filteredList, () => {
  pagination.total = filteredList.value.length
  const maxPage = Math.ceil(pagination.total / pagination.pageSize) || 1
  if (pagination.current > maxPage) {
    pagination.current = 1
  }
}, { immediate: true })

function handleSearch() { pagination.current = 1 }
function handleReset() {
  Object.assign(search, { keyword: '', description: '' })
  activeCategory.value = 'ALL'
  pagination.current = 1
}

// ===== 加载数据 =====
async function fetchAllTools() {
  loading.value = true
  try {
    const res: any = await getTools(projectId.value, { page: 1, pageSize: 10000 })
    allTools.value = res.data?.items || []
  } catch { allTools.value = [] } finally { loading.value = false }
}

// ===== 批量操作 =====
const selectedIds = computed(() => selectedRows.value.map((r: any) => r.id))
function handleSelectionChange(rows: any[]) { selectedRows.value = rows }
function clearSelection() { selectedRows.value = [] }
function handleBatchAction(key: string) {
  if (key === 'delete') handleBatchDelete()
  else if (key === 'group') openBatchGroup()
}
function handleBatchDelete() {
  ElMessageBox.confirm(
    `确定删除选中的 ${selectedIds.value.length} 个工具方法？`,
    '批量删除', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' },
  ).then(async () => {
    for (const id of selectedIds.value) {
      await deleteTool(projectId.value, id)
    }
    ElMessage.success('删除成功')
    clearSelection()
    fetchAllTools()
  }).catch(() => {})
}

function handleDelete(record: any) {
  ElMessageBox.confirm(`确定删除工具方法「${record.name}」？`, '确认删除', { type: 'warning' })
    .then(async () => { await deleteTool(projectId.value, record.id); ElMessage.success('删除成功'); fetchAllTools() })
    .catch(() => {})
}

// ===== 批量修改分组 =====
const batchGroupVisible = ref(false)
const batchGroupTarget = ref<string>('')
const availableCategories = computed(() =>
  categoryTree.value.filter((c) => !c.isSystem).map((c) => ({ id: c.id, name: c.name }))
)
function openBatchGroup() {
  batchGroupTarget.value = ''
  batchGroupVisible.value = true
}
async function applyBatchGroup() {
  if (!batchGroupTarget.value) { ElMessage.warning('请输入或选择目标分组'); return }
  try {
    for (const tool of selectedRows.value) {
      await updateTool(projectId.value, tool.id, { ...tool, category: batchGroupTarget.value })
    }
    ElMessage.success(`已成功将 ${selectedRows.value.length} 条工具方法的分组修改为「${batchGroupTarget.value}」`)
    batchGroupVisible.value = false
    clearSelection()
    fetchAllTools()
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '操作失败') }
}

// ===== 表字段调整 =====
const defaultColumns: ColumnItem[] = [
  { key: 'id', label: 'ID', locked: true, visible: true },
  { key: 'name', label: '工具方法', locked: true, visible: true },
  { key: 'category', label: '分组', locked: false, visible: true },
  { key: 'desc', label: '描述', locked: false, visible: true },
  { key: 'refCount', label: '被引用次数', locked: false, visible: true },
  { key: 'createTime', label: '创建时间', locked: false, visible: true },
  { key: 'action', label: '操作', locked: true, visible: true },
]
const columns = ref<ColumnItem[]>(defaultColumns.map((c) => ({ ...c })))
function isColVisible(key: string) {
  return columns.value.find((c) => c.key === key)?.visible ?? false
}
function resetColumns() {
  columns.value = defaultColumns.map((c) => ({ ...c }))
}

// ===== 在线测试弹窗 =====
const testVisible = ref(false)
const testLoading = ref(false)
const testResult = ref<any>(null)
const testParams = ref<any[]>([])
const testValues = reactive<Record<string, string>>({})
const currentTestTool = ref<any>(null)

function openTest(record: any) {
  currentTestTool.value = record
  testResult.value = null
  testParams.value = []
  Object.keys(testValues).forEach((k) => delete testValues[k])
  // 解析 paramDefinitions
  if (record.paramDefinitions) {
    try {
      const arr = JSON.parse(record.paramDefinitions)
      if (Array.isArray(arr)) {
        testParams.value = arr.map((p: any) => ({
          name: p.name || '',
          type: p.type || '',
          required: p.required !== false,
          defaultValue: p.defaultValue || '',
        }))
        arr.forEach((p: any) => {
          testValues[p.name] = p.defaultValue || ''
        })
      }
    } catch { /* ignore */ }
  }
  testVisible.value = true
}

async function handleTest() {
  if (!currentTestTool.value) return
  testLoading.value = true
  testResult.value = null
  try {
    const res: any = await testTool(projectId.value, currentTestTool.value.id, {
      testInput: JSON.stringify(testValues),
    })
    testResult.value = res.data
  } catch (e: any) {
    testResult.value = { success: 0, error: e?.response?.data?.message || e?.message || '执行失败' }
  } finally { testLoading.value = false }
}

onMounted(() => {
  fetchAllTools()
  document.addEventListener('click', closeContextMenu)
})
onBeforeUnmount(() => {
  document.removeEventListener('click', closeContextMenu)
})
</script>

<template>
  <div>
    <PageHeader title="工具方法">
      <el-button v-if="hasPermission('project:tool:add')" type="primary"
        @click="router.push(`/project/${projectId}/tools/new`)">+ 新建工具方法</el-button>
    </PageHeader>

    <div class="kw-layout">
      <!-- 左侧分组树 -->
      <div class="module-panel" @contextmenu="handleBlankContextmenu">
        <div class="module-head">
          <span class="module-title">分组</span>
        </div>
        <div class="module-tree-search">
          <el-input
            v-model="groupSearch"
            placeholder="搜索分组..."
            clearable
            size="small"
            prefix-icon="Search"
          />
        </div>
        <div class="module-tree">
          <div
            v-for="node in filteredCategoryTree"
            :key="node.id"
            :class="['module-tree-node', { active: activeCategory === node.id }]"
            @click="onCategoryClick(node)"
            @contextmenu.prevent.stop="handleNodeContextmenu($event, node)"
          >
            <span class="module-name">{{ node.name }}</span>
            <span v-if="node.isSystem" class="module-lock" title="系统默认分组">🔒</span>
            <span class="module-count">{{ node.count }}</span>
          </div>
        </div>
      </div>

      <!-- 右侧内容 -->
      <div class="kw-content">
        <ProSearchCard :loading="loading" @search="handleSearch" @reset="handleReset">
          <div class="pro-search-field">
            <span class="pro-search-label">工具方法</span>
            <el-input v-model="search.keyword" placeholder="输入工具方法名称" clearable
              style="width: 180px" @keyup.enter="handleSearch" />
          </div>
          <div class="pro-search-field">
            <span class="pro-search-label">描述</span>
            <el-input v-model="search.description" placeholder="输入描述" clearable
              style="width: 180px" @keyup.enter="handleSearch" />
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
          v-if="hasPermission('project:tool:edit')"
          :selected-count="selectedIds.length"
          :actions="[
            { key: 'group', label: '批量修改分组' },
            { key: 'delete', label: '批量删除', danger: true },
          ]"
          @action="handleBatchAction"
          @clear="clearSelection"
        />

        <el-table :data="pagedList" v-loading="loading" border stripe style="width: 100%"
          @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="45" />
          <el-table-column v-if="isColVisible('id')" prop="id" label="ID" width="70" />
          <el-table-column v-if="isColVisible('name')" prop="name" label="工具方法" width="350" show-overflow-tooltip />
          <el-table-column v-if="isColVisible('category')" label="分组" width="160">
            <template #default="{ row }">
              <span v-if="row.category && row.category !== 'CUSTOM' && row.category !== 'BUILTIN'">{{ row.category }}</span>
              <span v-else style="color: #c0c4cc">未分组</span>
            </template>
          </el-table-column>
          <el-table-column v-if="isColVisible('desc')" prop="description" label="描述" show-overflow-tooltip />
          <el-table-column v-if="isColVisible('refCount')" label="被引用次数" width="100">
            <template #default="{ row }">{{ row.referenceCount ?? 0 }}</template>
          </el-table-column>
          <el-table-column v-if="isColVisible('createTime')" label="创建时间" width="120">
            <template #default="{ row }">{{ row.createdAt?.substring(0, 10) }}</template>
          </el-table-column>
          <el-table-column v-if="isColVisible('action')" label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button v-if="hasPermission('project:tool:edit')" type="primary" link size="small"
                @click="router.push(`/project/${projectId}/tools/${row.id}/edit`)">编辑</el-button>
              <el-button v-if="hasPermission('project:tool:test')" type="primary" link size="small"
                @click="openTest(row)">调试</el-button>
              <el-button v-if="hasPermission('project:tool:delete')" type="danger" link size="small"
                @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <ProPagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          @change="(p: number) => { pagination.current = p }"
        />
      </div>
    </div>

    <!-- 在线测试弹窗 -->
    <el-dialog v-model="testVisible" title="在线调试" width="720px">
      <!-- 基础信息 -->
      <div class="test-info-section">
        <h4 class="test-section-title">基础信息</h4>
        <div class="test-info-grid">
          <div class="test-info-item">
            <span class="test-info-label">工具方法：</span>
            {{ currentTestTool?.name }}
          </div>
          <div class="test-info-item">
            <span class="test-info-label">分组：</span>
            {{ currentTestTool?.category && currentTestTool?.category !== 'CUSTOM' && currentTestTool?.category !== 'BUILTIN' ? currentTestTool.category : '未分组' }}
          </div>
          <div v-if="currentTestTool?.description" class="test-info-item full">
            <span class="test-info-label">描述：</span>
            {{ currentTestTool?.description }}
          </div>
        </div>
      </div>

      <!-- 代码预览 -->
      <div v-if="currentTestTool?.code" class="test-code-section">
        <h4 class="test-section-title">代码</h4>
        <div class="test-code-viewer">
          <pre><code>{{ currentTestTool?.code }}</code></pre>
        </div>
      </div>

      <!-- 测试参数 -->
      <div v-if="testParams.length > 0" class="test-params-section">
        <h4 class="test-section-title">测试参数</h4>
        <el-table :data="testParams" border size="small">
          <el-table-column label="参数名" width="140">
            <template #default="{ row }">
              <code>{{ row.name }}</code>
              <span v-if="row.required" style="color: var(--el-color-danger); margin-left: 4px">*</span>
              <el-tag v-if="row.type" size="small" type="info" style="margin-left: 6px">{{ row.type }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="测试值">
            <template #default="{ row }">
              <el-input v-model="testValues[row.name]" :placeholder="row.required ? '必填' : '可选'"
                size="small" style="width: 240px" />
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 执行结果 -->
      <div v-if="testResult" class="test-result-section">
        <h4 class="test-section-title">执行结果</h4>
        <div class="test-result-meta">
          <span v-if="testResult.executionTimeMs != null">耗时：<b style="color: var(--el-color-success)">{{ testResult.executionTimeMs }}ms</b></span>
          <span>状态：<el-tag :type="testResult.success === 1 ? 'success' : 'danger'" size="small">{{ testResult.success === 1 ? '成功' : '失败' }}</el-tag></span>
        </div>
        <div class="test-output">
          <pre>{{ testResult.output || testResult.error }}</pre>
        </div>
      </div>

      <template #footer>
        <el-button @click="testVisible = false">关闭</el-button>
        <el-button type="primary" :loading="testLoading" @click="handleTest">▶ 执行测试</el-button>
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
        <!-- 空白区域右键 -->
        <template v-if="!contextNode">
          <div class="context-menu-item" @click="contextCreateGroup">新建分组</div>
        </template>
        <!-- 系统分组右键：仅清空 -->
        <template v-else-if="contextNode.isSystem">
          <div class="context-menu-item danger" @click="contextClear">清空关键字</div>
        </template>
        <!-- 用户分组右键 -->
        <template v-else>
          <div class="context-menu-item" @click="contextCreateGroup">新建子分组</div>
          <div class="context-menu-divider" />
          <div class="context-menu-item" @click="contextEdit">编辑</div>
          <div class="context-menu-item danger" @click="contextClear">清空关键字</div>
          <div class="context-menu-item danger" @click="contextDelete">删除</div>
        </template>
      </div>
    </Teleport>

    <!-- 批量修改分组弹窗 -->
    <el-dialog v-model="batchGroupVisible" title="批量修改分组" width="420px">
      <p style="font-size: 13px; color: var(--el-text-color-secondary, #606266); margin-bottom: 16px">
        将已选中的 <b style="color: var(--el-color-primary, #409eff)">{{ selectedRows.length }}</b> 条工具方法的分组修改为：
      </p>
      <el-form-item label="目标分组" required>
        <el-select
          v-model="batchGroupTarget"
          placeholder="请选择或输入分组名称"
          filterable
          allow-create
          default-first-option
          style="width: 100%"
        >
          <el-option v-for="cat in availableCategories" :key="cat.id" :value="cat.name" :label="cat.name" />
        </el-select>
      </el-form-item>
      <div v-if="selectedRows.length" class="batch-preview">
        <div class="batch-preview-title">以下工具方法的分组将被修改：</div>
        <div v-for="row in selectedRows" :key="row.id" class="batch-preview-item">
          <span style="color: var(--el-text-color-secondary, #606266)">{{ row.name }}</span>
          <span class="batch-preview-arrow">{{ row.category || '未分组' }}</span>
          <span style="color: var(--el-text-color-disabled, #c0c4cc)">→</span>
          <span style="color: var(--el-color-primary, #409eff); font-weight: 500; margin-left: 4px">
            {{ batchGroupTarget || '请选择' }}
          </span>
        </div>
      </div>
      <template #footer>
        <el-button @click="batchGroupVisible = false">取消</el-button>
        <el-button type="primary" @click="applyBatchGroup">确认修改</el-button>
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
  border: 1px solid var(--el-border-color-light, #ebeef5);
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
  color: var(--el-text-color-primary, #303133);
}
.module-tree-search {
  margin-top: 8px;
  margin-bottom: 8px;
}
.module-tree {
  max-height: 560px;
  overflow-y: auto;
  margin-top: 8px;
}
.module-tree-node {
  display: flex;
  align-items: center;
  flex: 1;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 13px;
  gap: 6px;
  width: 100%;
  cursor: pointer;
  transition: background 0.15s;
}
.module-tree-node:hover {
  background: var(--el-fill-color-light, #f5f7fa);
}
.module-tree-node.active {
  background: var(--el-color-primary-light-9, #ecf5ff);
  color: var(--el-color-primary, #409eff);
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
  color: var(--el-text-color-secondary, #909399);
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
.cell-truncate {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.test-info-section {
  margin-bottom: 16px;
}
.test-info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px 24px;
}
.test-info-item {
  display: flex;
  align-items: baseline;
  font-size: 13px;
}
.test-info-item.full {
  grid-column: 1 / -1;
}
.test-info-label {
  color: var(--el-text-color-secondary, #909399);
  flex-shrink: 0;
  min-width: 56px;
}
.test-code-section {
  margin-bottom: 16px;
}
.test-code-viewer {
  background: #1e1e1e;
  border-radius: 4px;
  padding: 12px;
  max-height: 200px;
  overflow: auto;
}
.test-code-viewer pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}
.test-code-viewer code {
  color: #d4d4d4;
  font-size: 12px;
  font-family: Consolas, 'Courier New', monospace;
}
.test-params-section {
  margin-bottom: 16px;
}
.test-section-title {
  font-size: 13px;
  font-weight: 600;
  margin: 0 0 12px;
}
.test-result-section {
  margin-top: 16px;
}
.test-result-meta {
  display: flex;
  gap: 16px;
  margin-bottom: 8px;
  font-size: 13px;
}
.test-output {
  background: #1e1e1e;
  border-radius: 4px;
  padding: 12px;
  min-height: 60px;
}
.test-output pre {
  margin: 0;
  color: #d4d4d4;
  font-size: 12px;
  font-family: Consolas, 'Courier New', monospace;
  white-space: pre-wrap;
  word-break: break-all;
}
.batch-preview {
  margin-top: 12px;
  padding: 12px;
  background: var(--el-fill-color-light, #f5f7fa);
  border-radius: 4px;
  font-size: 13px;
  max-height: 120px;
  overflow-y: auto;
}
.batch-preview-title {
  color: var(--el-text-color-secondary, #909399);
  margin-bottom: 6px;
}
.batch-preview-item {
  padding: 2px 0;
}
.batch-preview-arrow {
  margin: 0 6px;
  color: var(--el-text-color-disabled, #c0c4cc);
  font-size: 12px;
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
.context-menu-divider {
  height: 1px;
  background: #ebeef5;
  margin: 4px 0;
}
.module-lock {
  font-size: 10px;
  color: #c0c4cc;
  flex-shrink: 0;
  margin-left: 2px;
}
</style>
