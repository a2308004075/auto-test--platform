<!--
 @author HXN
 @date 2026-08-23
 @description 自动化用例列表视图
-->
<script setup lang="ts">
/**
 * 自动化用例列表 - M8
 * 左侧分组树 + 右侧高级搜索 + 批量操作 + 分页表格
 * 对齐原型 case-list.html
 */
import { ref, reactive, onMounted, onBeforeUnmount, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAutoCases, deleteAutoCase, toggleAutoCaseStatus, debugAutoCase, getAutoCaseGroups, createAutoCaseGroup, updateAutoCaseGroup, deleteAutoCaseGroup, clearGroupAutoCases, clearProjectAutoCases } from '@/api/autoCase'
import { getAutoSuites } from '@/api/autoSuite'
import { getEnvironments } from '@/api/environment'
import PageHeader from '@/components/PageHeader/index.vue'
import ProSearchCard from '@/components/ProSearchCard/index.vue'
import BatchBar from '@/components/BatchBar/index.vue'
import ProPagination from '@/components/ProPagination/index.vue'
import CaseRequirementPanel from '@/components/CaseRequirementPanel/index.vue'
import CaseDefectPanel from '@/components/CaseDefectPanel/index.vue'
import { useDict } from '@/composables/useDict'
import { usePermission } from '@/composables/usePermission'

const route = useRoute()
const router = useRouter()
const projectId = computed(() => Number(route.params.id))
const { hasPermission } = usePermission()
const { options: priorityOptions } = useDict('priority')
const { options: statusOptions } = useDict('is_active')

// ===== 列表数据 =====
const loading = ref(false)
const list = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const selectedRows = ref<any[]>([])

// ===== 搜索条件 =====
const search = reactive({ name: '', autoSuiteId: undefined as number | undefined, priority: '', status: '' })

// ===== 自动化套件列表 =====
const suites = ref<any[]>([])
async function fetchSuites() {
  try {
    const res: any = await getAutoSuites(projectId.value, { pageSize: 200 })
    suites.value = res.data?.items || []
  } catch { suites.value = [] }
}

// ===== 分组 =====
const groups = ref<any[]>([])
const activeGroupId = ref<number>(0) // 0 = 全部，-1 = 未分组，正数 = 分组 ID
const filterText = ref('')
// 用户分组（非系统），用于批量移动
const userGroups = computed(() => groups.value.filter((g) => g.isSystem !== 1))
// 未分组计数 ≈ 当前总数 − 顶层用户分组递归计数和（顶层 caseCount 已含子孙分组）
const ungroupedCount = computed(() => {
  const topUserGroups = groups.value.filter((g) => g.isSystem !== 1 && (g.parentId ?? null) === null)
  const groupedSum = topUserGroups.reduce((acc, g) => acc + (g.caseCount || 0), 0)
  return Math.max(0, pagination.total - groupedSum)
})
// 分组树：全部(虚拟) + 未分组(虚拟) + 用户分组按 parentId 建树
const groupTree = computed(() => {
  const userItems = groups.value.filter((g) => g.isSystem !== 1)
  const buildTree = (parentId: number | null): any[] =>
    userItems
      .filter((g) => (g.parentId ?? null) === parentId)
      .map((g) => ({ ...g, children: buildTree(g.id) }))
  return [
    { id: 0, name: '全部', isSystem: 1, caseCount: pagination.total, children: [] },
    { id: -1, name: '未分组', isSystem: 1, caseCount: ungroupedCount.value, children: [] },
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
function filterNode(value: string, data: any) {
  if (!value) return true
  return data.name.toLowerCase().includes(value.toLowerCase())
}

function onGroupNodeClick(data: any) {
  selectGroup(data.id)
}

async function fetchGroups() {
  try {
    const res: any = await getAutoCaseGroups(projectId.value)
    groups.value = res.data || []
  } catch { groups.value = [] }
}

async function fetchList() {
  loading.value = true
  try {
    // activeGroupId：0=全部（不传）；-1=未分组（后端 groupId=0）；正数=分组 ID
    const groupIdParam = activeGroupId.value === 0 ? undefined : activeGroupId.value === -1 ? 0 : activeGroupId.value
    const res: any = await getAutoCases(projectId.value, {
      groupId: groupIdParam,
      autoSuiteId: search.autoSuiteId || undefined,
      keyword: search.name || undefined,
      priority: search.priority || undefined,
      status: search.status || undefined,
      page: pagination.current, pageSize: pagination.pageSize,
    })
    list.value = res.data?.items || []
    pagination.total = res.data?.total || 0
  } catch { list.value = [] } finally { loading.value = false }
}

function selectGroup(id: number) {
  activeGroupId.value = id === activeGroupId.value ? 0 : id
  pagination.current = 1
  fetchList()
}

function handleSearch() { pagination.current = 1; fetchList() }
function handleReset() {
  Object.assign(search, { name: '', autoSuiteId: undefined, priority: '', status: '' })
  handleSearch()
}

// ===== 右键菜单 =====
const contextMenuVisible = ref(false)
const contextMenuPos = reactive({ x: 0, y: 0 })
const contextGroup = ref<any>(null)

function handleNodeContextmenu(e: MouseEvent, data: any) {
  e.preventDefault()
  e.stopPropagation()
  // 系统分组（全部/未分组虚拟节点）右键显示清空菜单
  contextGroup.value = data
  contextMenuPos.x = e.clientX
  contextMenuPos.y = e.clientY
  contextMenuVisible.value = true
}

function handleBlankContextmenu(e: MouseEvent) {
  e.preventDefault()
  contextGroup.value = null
  contextMenuPos.x = e.clientX
  contextMenuPos.y = e.clientY
  contextMenuVisible.value = true
}

function closeContextMenu() {
  contextMenuVisible.value = false
  contextGroup.value = null
}

function contextCreateGroup() {
  if (contextGroup.value) {
    openCreateGroup(contextGroup.value.id)
  } else {
    openCreateGroup()
  }
  closeContextMenu()
}

function contextCreateChild() {
  if (contextGroup.value) openCreateGroup(contextGroup.value.id)
  closeContextMenu()
}

function contextEdit() {
  if (contextGroup.value) openEditGroup(contextGroup.value)
  closeContextMenu()
}

function contextDelete() {
  if (contextGroup.value) handleDeleteGroup(contextGroup.value)
  closeContextMenu()
}

function contextClear() {
  if (!contextGroup.value) return
  const g = contextGroup.value
  closeContextMenu()
  const isAll = g.id === 0
  const isUngrouped = g.id === -1
  ElMessageBox.confirm(
    isAll
      ? '确定清空项目下的所有自动化用例？此操作不可恢复。'
      : isUngrouped
        ? '确定清空「未分组」中的所有自动化用例？此操作不可恢复。'
        : `确定清空分组「${g.name}」及其子分组中的所有自动化用例？此操作不可恢复。`,
    '确认清空',
    { type: 'warning', confirmButtonText: '清空', cancelButtonText: '取消' }
  )
    .then(async () => {
      if (isAll) {
        await clearProjectAutoCases(projectId.value)
      } else {
        // 未分组（-1）后端语义为 groupId=0
        await clearGroupAutoCases(projectId.value, isUngrouped ? 0 : g.id)
      }
      ElMessage.success('已清空')
      fetchGroups()
      fetchList()
    })
    .catch(() => {})
}

// ===== 批量操作 =====
const selectedIds = computed(() => selectedRows.value.map((r: any) => r.id))
function handleSelectionChange(rows: any[]) { selectedRows.value = rows }

function handleBatchAction(key: string) {
  if (key === 'delete') handleBatchDelete()
  else if (key === 'enable') handleBatchToggle(true)
  else if (key === 'disable') handleBatchToggle(false)
  else if (key === 'move') batchMoveVisible.value = true
}
function clearSelection() { selectedRows.value = [] }

function handleBatchDelete() {
  ElMessageBox.confirm(
    `确定删除选中的 ${selectedIds.value.length} 条自动化用例？`,
    '批量删除', { type: 'warning' },
  ).then(async () => {
    for (const id of selectedIds.value) {
      await deleteAutoCase(projectId.value, id)
    }
    ElMessage.success('删除成功')
    clearSelection()
    fetchGroups(); fetchList()
  }).catch(() => {})
}

async function handleBatchToggle(enable: boolean) {
  const target = enable ? '启用' : '禁用'
  try {
    for (const row of selectedRows.value) {
      const shouldToggle = enable ? row.isActive !== 1 : row.isActive === 1
      if (shouldToggle) {
        await toggleAutoCaseStatus(projectId.value, row.id)
      }
    }
    ElMessage.success(`批量${target}成功`)
    fetchList()
  } catch { ElMessage.error(`批量${target}失败`) }
}

// 批量改组
const batchMoveVisible = ref(false)
const batchMoveTarget = ref<number | null>(null)
async function handleBatchMove() {
  if (!batchMoveTarget.value) { ElMessage.warning('请选择目标分组'); return }
  try {
    for (const id of selectedIds.value) {
      const { updateAutoCase } = await import('@/api/autoCase')
      await updateAutoCase(projectId.value, id, { groupId: batchMoveTarget.value })
    }
    ElMessage.success('移动成功')
    batchMoveVisible.value = false
    batchMoveTarget.value = null
    clearSelection()
    fetchGroups(); fetchList()
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '操作失败') }
}

// ===== 单条操作 =====
function openCreate() {
  router.push(`/project/${projectId.value}/auto-cases/new?autoSuiteId=${search.autoSuiteId || ''}`)
}

function handleEdit(record: any) {
  router.push(`/project/${projectId.value}/auto-cases/${record.id}/edit`)
}

async function handleToggleStatus(record: any) {
  try {
    await toggleAutoCaseStatus(projectId.value, record.id)
    ElMessage.success(record.isActive === 1 ? '已禁用' : '已启用')
    fetchList()
  } catch { ElMessage.error('操作失败') }
}

function handleDelete(record: any) {
  ElMessageBox.confirm(`确定删除自动化用例「${record.name}」？`, '确认删除', { type: 'warning' })
    .then(async () => { await deleteAutoCase(projectId.value, record.id); ElMessage.success('删除成功'); fetchGroups(); fetchList() })
    .catch(() => {})
}

function handleDebug(record: any) {
  debugRecord.value = record
  debugResult.value = null
  debugLoading.value = false
  debugEnvId.value = undefined
  debugVisible.value = true
  // 加载环境列表
  loadDebugEnvs()
}

// ===== 关联抽屉（关联需求/关联缺陷） =====
const relationDrawerVisible = ref(false)
const relationCase = ref<any>(null)

function handleRelation(record: any) {
  relationCase.value = record
  relationDrawerVisible.value = true
}

// ===== 调试弹窗 =====
const debugVisible = ref(false)
const debugLoading = ref(false)
const debugRecord = ref<any>(null)
const debugResult = ref<any>(null)
const debugEnvId = ref<number | undefined>(undefined)
const debugEnvs = ref<any[]>([])

async function loadDebugEnvs() {
  try {
    const res: any = await getEnvironments(projectId.value)
    debugEnvs.value = res.data || []
  } catch { debugEnvs.value = [] }
}

async function handleRunDebug() {
  if (!debugRecord.value) return
  debugLoading.value = true
  debugResult.value = null
  try {
    const res: any = await debugAutoCase(projectId.value, debugRecord.value.id, {
      environmentId: debugEnvId.value,
    })
    debugResult.value = res.data
  } catch (e: any) {
    debugResult.value = { status: 'ERROR', message: e?.response?.data?.message || '调试执行失败', stepLogs: [] }
  } finally {
    debugLoading.value = false
  }
}

function debugStatusType(status: string) {
  if (status === 'PASSED') return 'success'
  if (status === 'FAILED') return 'danger'
  if (status === 'ERROR') return 'danger'
  return 'info'
}

// ===== 分组 CRUD =====
const groupModalVisible = ref(false)
const editingGroupId = ref<number>(0)
const groupForm = reactive({ name: '', description: '', parentId: null as number | null })

function openCreateGroup(parentId?: number | null) {
  editingGroupId.value = 0
  Object.assign(groupForm, { name: '', description: '', parentId: parentId ?? null })
  groupModalVisible.value = true
}
function openEditGroup(g: any) {
  if (g.isSystem === 1) { ElMessage.info('系统分组不可编辑'); return }
  editingGroupId.value = g.id
  Object.assign(groupForm, { name: g.name, description: g.description || '', parentId: g.parentId ?? null })
  groupModalVisible.value = true
}
async function handleGroupSubmit() {
  if (!groupForm.name) { ElMessage.warning('请输入分组名称'); return }
  try {
    if (editingGroupId.value) {
      await updateAutoCaseGroup(projectId.value, editingGroupId.value, groupForm)
      ElMessage.success('更新成功')
    } else {
      await createAutoCaseGroup(projectId.value, groupForm)
      ElMessage.success('创建成功')
    }
    groupModalVisible.value = false
    fetchGroups()
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '操作失败') }
}
function handleDeleteGroup(g: any) {
  if (g.isSystem === 1) { ElMessage.info('系统分组不可删除'); return }
  ElMessageBox.confirm(`确定删除分组「${g.name}」？删除后该分组下的自动化用例将自动归入「未分组」。`, '确认删除', { type: 'warning' })
    .then(async () => { await deleteAutoCaseGroup(projectId.value, g.id); ElMessage.success('删除成功'); fetchGroups() })
    .catch(() => {})
}

// ===== 标签解析 =====
function parseTags(raw?: string): string[] {
  if (!raw) return []
  try {
    const arr = JSON.parse(raw)
    return Array.isArray(arr) ? arr : []
  } catch { return [] }
}

// ===== 优先级 =====
const priorityTypeMap: Record<string, string> = { '高': 'danger', '中': 'warning', '低': 'info', P0: 'danger', P1: 'warning', P2: '', P3: 'info' }

// ===== 视图切换 =====
const viewMode = ref<'list' | 'card'>('list')
watch(viewMode, () => { clearSelection() })

// ===== 生命周期 =====
const treeRef = ref()
function onDocClick() { closeContextMenu() }
onMounted(() => {
  fetchSuites(); fetchGroups(); fetchList()
  document.addEventListener('click', onDocClick)
})
onBeforeUnmount(() => {
  document.removeEventListener('click', onDocClick)
})
</script>

<template>
  <div>
    <PageHeader title="自动化用例">
      <el-button v-if="hasPermission('project:auto-case:add')" type="primary" @click="openCreate">+ 新建自动化用例</el-button>
    </PageHeader>

    <div class="case-layout">
      <!-- 左侧分组 -->
      <div class="group-panel" @contextmenu="handleBlankContextmenu">
        <div class="group-head">
          <span class="group-title">分组</span>
        </div>
        <div class="tree-search">
          <el-input v-model="filterText" size="small" placeholder="搜索分组..." clearable @input="(v: string) => treeRef?.filter(v)" />
        </div>
        <div class="group-tree">
          <el-tree
            ref="treeRef"
            :data="filteredGroupTree"
            node-key="id"
            :props="{ label: 'name', children: 'children' }"
            :default-expand-all="true"
            :expand-on-click-node="false"
            :filter-node-method="filterNode"
            @node-click="onGroupNodeClick"
          >
            <template #default="{ data }">
              <div
                :class="['group-tree-node', { active: activeGroupId === data.id }]"
                @contextmenu.stop="handleNodeContextmenu($event, data)"
              >
                <span class="group-name">{{ data.name }}</span>
                <span class="group-count">{{ data.caseCount ?? 0 }}</span>
                <span v-if="data.isSystem === 1" class="group-lock" title="系统默认分组">&#x1F512;</span>
              </div>
            </template>
          </el-tree>
        </div>
      </div>

      <!-- 右侧内容 -->
      <div class="case-content">
        <ProSearchCard :loading="loading" @search="handleSearch" @reset="handleReset">
          <div class="pro-search-field">
            <span class="pro-search-label">自动化用例名称</span>
            <el-input v-model="search.name" placeholder="搜索自动化用例名称" clearable style="width: 180px" @keyup.enter="handleSearch" />
          </div>
          <div class="pro-search-field">
            <span class="pro-search-label">所属自动化套件</span>
            <el-select v-model="search.autoSuiteId" placeholder="全部自动化套件" clearable style="width: 160px">
              <el-option v-for="s in suites" :key="s.id" :value="s.id" :label="s.name" />
            </el-select>
          </div>
          <div class="pro-search-field">
            <span class="pro-search-label">优先级</span>
            <el-select v-model="search.priority" placeholder="全部" clearable style="width: 100px">
              <el-option v-for="p in priorityOptions" :key="p.value" :value="p.value" :label="p.label" />
            </el-select>
          </div>
          <template #collapse>
            <div class="pro-search-field">
              <span class="pro-search-label">状态</span>
              <el-select v-model="search.status" placeholder="全部状态" clearable style="width: 120px">
                <el-option v-for="s in statusOptions" :key="s.value" :value="s.value" :label="s.label" />
              </el-select>
            </div>
          </template>
        </ProSearchCard>

        <div class="table-toolbar">
          <div style="display:flex;gap:6px;align-items:center;margin-left:auto">
            <el-radio-group v-model="viewMode" size="small">
              <el-radio-button value="list">列表</el-radio-button>
              <el-radio-button value="card">卡片</el-radio-button>
            </el-radio-group>
          </div>
        </div>

        <BatchBar
          v-if="viewMode === 'list'"
          :selected-count="selectedIds.length"
          :actions="[
            { key: 'enable', label: '批量启用' },
            { key: 'disable', label: '批量禁用' },
            { key: 'move', label: '批量修改分组' },
            { key: 'delete', label: '批量删除', danger: true },
          ]"
          @action="handleBatchAction"
          @clear="clearSelection"
        />

        <!-- 列表视图 -->
        <el-table v-if="viewMode === 'list'" :data="list" v-loading="loading" border stripe style="width: 100%" @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="45" />
          <el-table-column prop="name" label="自动化用例名称" min-width="180" show-overflow-tooltip>
            <template #default="{ row }">
              <el-button type="primary" link @click="handleEdit(row)">{{ row.name }}</el-button>
            </template>
          </el-table-column>
          <el-table-column label="所属自动化套件" width="160" show-overflow-tooltip>
            <template #default="{ row }">{{ suites.find((s: any) => s.id === row.autoSuiteId)?.name || '--' }}</template>
          </el-table-column>
          <el-table-column label="优先级" width="80">
            <template #default="{ row }">
              <el-tag :type="(priorityTypeMap[row.priority] || 'info') as any" size="small">{{ row.priority }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.isActive === 1 ? 'success' : 'info'" size="small">{{ row.isActive === 1 ? '启用' : '禁用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="标签" width="180">
            <template #default="{ row }">
              <el-tag v-for="(tag, i) in parseTags(row.tags)" :key="i" size="small" style="margin-right:4px">{{ tag }}</el-tag>
              <span v-if="!parseTags(row.tags).length" style="color:#c0c4cc">--</span>
            </template>
          </el-table-column>
          <el-table-column label="最近执行" width="100">
            <template #default>
              <span style="color:#c0c4cc">-</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="250" fixed="right">
            <template #default="{ row }">
              <el-button v-if="hasPermission('project:auto-case:edit')" type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button type="primary" link size="small" @click="handleDebug(row)">调试</el-button>
              <el-button type="primary" link size="small" @click="handleRelation(row)">关联</el-button>
              <el-button v-if="hasPermission('project:auto-case:toggle')" type="primary" link size="small" @click="handleToggleStatus(row)">{{ row.isActive === 1 ? '禁用' : '启用' }}</el-button>
              <el-button v-if="hasPermission('project:auto-case:delete')" type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
          <template #empty>
            <div style="padding:48px 20px;text-align:center;color:#909399">
              <div>暂无数据</div>
            </div>
          </template>
        </el-table>

        <!-- 卡片视图 -->
        <div v-else v-loading="loading" class="case-card-grid">
          <div v-if="list.length === 0" class="card-empty">
            <div>暂无数据</div>
          </div>
          <div v-for="item in list" :key="item.id" class="case-card" @click="handleEdit(item)">
            <div class="case-card-header">
              <span class="case-card-name">{{ item.name }}</span>
              <el-tag :type="(priorityTypeMap[item.priority] || 'info') as any" size="small">{{ item.priority }}</el-tag>
            </div>
            <div class="case-card-body">
              <div class="case-card-info">
                <span class="info-label">自动化套件</span>
                <span class="info-value">{{ suites.find((s: any) => s.id === item.autoSuiteId)?.name || '--' }}</span>
              </div>
              <div class="case-card-info">
                <span class="info-label">状态</span>
                <el-tag :type="item.isActive === 1 ? 'success' : 'info'" size="small">{{ item.isActive === 1 ? '启用' : '禁用' }}</el-tag>
              </div>
              <div v-if="parseTags(item.tags).length" class="case-card-tags">
                <el-tag v-for="(tag, i) in parseTags(item.tags)" :key="i" size="small" style="margin-right:4px">{{ tag }}</el-tag>
              </div>
            </div>
            <div class="case-card-footer">
              <el-button v-if="hasPermission('project:auto-case:edit')" type="primary" link size="small" @click.stop="handleEdit(item)">编辑</el-button>
              <el-button type="primary" link size="small" @click.stop="handleDebug(item)">调试</el-button>
              <el-button type="primary" link size="small" @click.stop="handleRelation(item)">关联</el-button>
              <el-button v-if="hasPermission('project:auto-case:toggle')" type="primary" link size="small" @click.stop="handleToggleStatus(item)">{{ item.isActive === 1 ? '禁用' : '启用' }}</el-button>
              <el-button v-if="hasPermission('project:auto-case:delete')" type="danger" link size="small" @click.stop="handleDelete(item)">删除</el-button>
            </div>
          </div>
        </div>

        <ProPagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          @change="(p: number) => { pagination.current = p; fetchList() }"
        />
      </div>
    </div>

    <!-- 分组新建/编辑弹窗 -->
    <el-dialog v-model="groupModalVisible" :title="editingGroupId ? '编辑分组' : '新建分组'" width="460px">
      <el-form label-position="top">
        <el-form-item label="分组名称" required>
          <el-input v-model="groupForm.name" placeholder="如：认证测试" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="groupForm.description" type="textarea" :rows="2" placeholder="可选，分组描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="groupModalVisible = false">取消</el-button>
        <el-button type="primary" @click="handleGroupSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 批量修改分组弹窗 -->
    <el-dialog v-model="batchMoveVisible" title="批量修改分组" width="420px">
      <p style="margin: 0 0 12px; color: #606266; font-size: 13px;">
        将选中的 <b style="color: #409eff">{{ selectedIds.length }}</b> 条自动化用例移动到：
      </p>
      <el-select v-model="batchMoveTarget" placeholder="选择目标分组" filterable style="width: 100%">
        <el-option v-for="g in userGroups" :key="g.id" :value="g.id" :label="g.name" />
      </el-select>
      <template #footer>
        <el-button @click="batchMoveVisible = false">取消</el-button>
        <el-button type="primary" @click="handleBatchMove">确认移动</el-button>
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
          <div v-if="hasPermission('project:auto-case:group')" class="context-menu-item" @click="contextCreateGroup">新建分组</div>
        </template>
        <!-- 系统分组（全部/未分组）右键：仅允许清空 -->
        <template v-else-if="contextGroup.isSystem === 1">
          <div class="context-menu-item danger" @click="contextClear">清空自动化用例</div>
        </template>
        <!-- 用户分组右键 -->
        <template v-else>
          <div v-if="hasPermission('project:auto-case:group')" class="context-menu-item" @click="contextCreateChild">新建子分组</div>
          <div v-if="hasPermission('project:auto-case:group')" class="context-menu-divider" />
          <div class="context-menu-item" @click="contextEdit">编辑</div>
          <div class="context-menu-item danger" @click="contextClear">清空自动化用例</div>
          <div class="context-menu-item danger" @click="contextDelete">删除</div>
        </template>
      </div>
    </Teleport>

    <!-- 自动化用例调试弹窗 -->
    <el-dialog v-model="debugVisible" title="自动化用例调试" width="720px" destroy-on-close>
      <div style="margin-bottom: 12px">
        <span style="font-weight: 600">{{ debugRecord?.name }}</span>
        <span v-if="debugRecord?.description" style="color: #909399; margin-left: 8px; font-size: 13px">{{ debugRecord.description }}</span>
      </div>
      <div style="display: flex; gap: 12px; align-items: center; margin-bottom: 16px">
        <span style="font-size: 13px; color: #606266">执行环境：</span>
        <el-select v-model="debugEnvId" placeholder="不选择环境" clearable style="width: 200px" size="small">
          <el-option v-for="env in debugEnvs" :key="env.id" :value="env.id" :label="env.name" />
        </el-select>
        <el-button type="primary" size="small" :loading="debugLoading" @click="handleRunDebug">
          {{ debugLoading ? '执行中...' : '执行调试' }}
        </el-button>
      </div>
      <!-- 调试结果 -->
      <div v-if="debugResult">
        <el-divider style="margin: 8px 0" />
        <div style="display: flex; gap: 12px; align-items: center; margin-bottom: 12px">
          <el-tag :type="debugStatusType(debugResult.status) as any" size="large">{{ debugResult.status }}</el-tag>
          <span style="color: #606266">{{ debugResult.message }}</span>
          <span style="margin-left: auto; color: #909399; font-size: 13px">耗时：{{ debugResult.durationMs }}ms</span>
        </div>
        <!-- 步骤日志 -->
        <div v-if="debugResult.stepLogs && debugResult.stepLogs.length" style="max-height: 300px; overflow-y: auto">
          <el-table :data="debugResult.stepLogs" border size="small" style="width: 100%">
            <el-table-column prop="stepName" label="步骤名称" min-width="150" show-overflow-tooltip />
            <el-table-column prop="phase" label="阶段" width="80" />
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="debugStatusType(row.status) as any" size="small">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="message" label="消息" min-width="150" show-overflow-tooltip />
            <el-table-column prop="durationMs" label="耗时" width="80" />
          </el-table>
        </div>
        <div v-else-if="debugResult.status !== 'ERROR'" style="text-align: center; color: #c0c4cc; padding: 20px">
          无步骤日志
        </div>
      </div>
      <template #footer>
        <el-button @click="debugVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 自动化用例关联抽屉（关联需求/关联缺陷） -->
    <el-drawer
      v-model="relationDrawerVisible"
      :title="`自动化用例关联 - ${relationCase?.name || ''}`"
      size="560px"
      destroy-on-close
    >
      <el-tabs type="card">
        <el-tab-pane label="关联需求" name="requirement">
          <CaseRequirementPanel
            :project-id="projectId"
            case-type="AUTO_CASE"
            :case-id="relationCase?.id"
          />
        </el-tab-pane>
        <el-tab-pane label="关联缺陷" name="defect">
          <CaseDefectPanel
            :project-id="projectId"
            target-type="AUTO_CASE"
            :target-id="relationCase?.id"
          />
        </el-tab-pane>
      </el-tabs>
    </el-drawer>
  </div>
</template>

<style scoped>
.case-layout {
  display: flex;
  gap: 16px;
  min-height: calc(100vh - 164px);
}
.group-panel {
  width: 220px;
  flex-shrink: 0;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.group-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.group-title {
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
.group-tree {
  flex: 1;
  overflow-y: auto;
}
.group-tree :deep(.el-tree-node__content) {
  height: auto;
  padding: 2px 0;
}
.group-tree-node {
  display: flex;
  align-items: center;
  flex: 1;
  padding: 2px 4px;
  border-radius: 4px;
  font-size: 13px;
  gap: 6px;
  width: 100%;
}
.group-tree-node:hover {
  background: #f5f7fa;
}
.group-tree-node.active {
  background: #ecf5ff;
  color: #409eff;
  font-weight: 500;
}
.group-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.group-count {
  font-size: 12px;
  color: #909399;
  flex-shrink: 0;
}
.group-lock {
  font-size: 10px;
  color: #c0c4cc;
  flex-shrink: 0;
  margin-left: 2px;
}
.case-content {
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
.context-menu-divider {
  height: 1px;
  background: #ebeef5;
  margin: 4px 0;
}

/* 卡片视图 */
.case-card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 12px;
  min-height: 200px;
}
.card-empty {
  grid-column: 1 / -1;
  padding: 48px 20px;
  text-align: center;
  color: #909399;
}
.case-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 14px 16px;
  cursor: pointer;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.case-card:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.12);
}
.case-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.case-card-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  min-width: 0;
  margin-right: 8px;
}
.case-card-body {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.case-card-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}
.info-label {
  color: #909399;
  min-width: 32px;
}
.info-value {
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.case-card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 2px;
}
.case-card-footer {
  display: flex;
  gap: 4px;
  margin-top: 10px;
  border-top: 1px solid #f5f7fa;
  padding-top: 8px;
}
</style>
