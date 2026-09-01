<!--
 @author HXN
 @date 2026-08-23 10:00
 @description 测试套件列表视图
-->
<script setup lang="ts">
/**
 * 测试套件列表 - M8
 * 左侧分组树 + 右侧表格 + 批量操作 + 右键菜单 + 通过率
 * 对齐原型 suite-list.html
 */
import { ref, reactive, onMounted, onBeforeUnmount, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getSuites, createSuite, updateSuite, deleteSuite,
  getSuiteGroups, createSuiteGroup, updateSuiteGroup, deleteSuiteGroup,
  getSuitePassRates, batchUpdateSuiteGroup, clearGroupSuites, clearProjectSuites,
} from '@/api/suite'
import PageHeader from '@/components/PageHeader/index.vue'
import BatchBar from '@/components/BatchBar/index.vue'
import ProPagination from '@/components/ProPagination/index.vue'
import ProSearchCard from '@/components/ProSearchCard/index.vue'
import { useDict } from '@/composables/useDict'
import { usePermission } from '@/composables/usePermission'

const route = useRoute()
const router = useRouter()
const projectId = computed(() => Number(route.params.id))
const { hasPermission } = usePermission()
const { options: priorityOptions } = useDict('priority')

// ===== 列表数据 =====
const loading = ref(false)
const list = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const selectedRows = ref<any[]>([])
const passRateMap = ref<Record<number, number>>({})

// ===== 搜索条件 =====
const search = reactive({ name: '', priority: '' })

// ===== 分组 =====
const groups = ref<any[]>([])
const activeGroupId = ref<number>(0) // 0 = 全部，-1 = 未分组，正数 = 分组 ID
const filterText = ref('')
const treeRef = ref()
const groupMap = computed<Record<number, any>>(() => {
  const m: Record<number, any> = {}
  groups.value.forEach((g: any) => { m[g.id] = g })
  return m
})

// 分组树构建：全部(虚拟) + 未分组(虚拟) + 用户分组按 parentId 建树
const groupTree = computed(() => {
  const userGroups = groups.value
  const buildTree = (parentId: number | null): any[] =>
    userGroups
      .filter((g: any) => (g.parentId ?? null) === parentId)
      .map((g: any) => ({ ...g, children: buildTree(g.id) }))

  const userTree = buildTree(null)
  // 仅统计顶层用户分组的递归套件数（后端已含后代计数）
  const topLevelCount = userTree.reduce((sum: number, g: any) => sum + (g.suiteCount || 0), 0)
  const ungroupedCount = Math.max(0, pagination.total - topLevelCount)

  return [
    { id: 0, name: '全部', isSystem: 1, suiteCount: pagination.total, children: [] },
    { id: -1, name: '未分组', isSystem: 1, suiteCount: ungroupedCount, children: [] },
    ...userTree,
  ]
})

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

function selectGroup(id: number) {
  activeGroupId.value = id === activeGroupId.value ? 0 : id
  pagination.current = 1
  clearSelection()
  fetchList()
}

async function fetchGroups() {
  try {
    const res: any = await getSuiteGroups(projectId.value)
    groups.value = res.data || []
  } catch { groups.value = [] }
}

async function fetchList() {
  loading.value = true
  try {
    // activeGroupId：0=全部（不传）；-1=未分组（后端 groupId=0）；正数=分组 ID
    const groupIdParam = activeGroupId.value === 0 ? undefined : activeGroupId.value === -1 ? 0 : activeGroupId.value
    const res: any = await getSuites(projectId.value, {
      keyword: search.name || undefined,
      groupId: groupIdParam,
      priority: search.priority || undefined,
      page: pagination.current,
      pageSize: pagination.pageSize,
    })
    list.value = res.data?.items || []
    pagination.total = res.data?.total || 0
    // 异步加载通过率
    fetchPassRates()
  } catch { list.value = [] } finally { loading.value = false }
}

async function fetchPassRates() {
  if (list.value.length === 0) return
  try {
    const ids = list.value.map((s: any) => s.id)
    const res: any = await getSuitePassRates(projectId.value, ids)
    const map: Record<number, number> = {}
    for (const item of (res.data || [])) {
      map[item.suiteId] = item.passRate
    }
    passRateMap.value = map
  } catch { /* ignore */ }
}

function handleSearch() { pagination.current = 1; fetchList() }
function handleReset() { Object.assign(search, { name: '', priority: '' }); handleSearch() }

// ===== 选中 & 批量 =====
const selectedIds = computed(() => selectedRows.value.map((r: any) => r.id))

function handleSelectionChange(rows: any[]) {
  selectedRows.value = rows
}

function clearSelection() {
  selectedRows.value = []
}

async function handleBatchAction(key: string) {
  if (key === 'delete') {
    await batchDelete()
  } else if (key === 'changeGroup') {
    batchGroupVisible.value = true
  }
}

async function batchDelete() {
  if (selectedIds.value.length === 0) return
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 个套件？其下所有用例将一并删除。`, '批量删除', { type: 'warning' })
    for (const id of selectedIds.value) {
      await deleteSuite(projectId.value, id)
    }
    ElMessage.success('批量删除成功')
    clearSelection()
    fetchGroups()
    fetchList()
  } catch { /* cancelled */ }
}

// ===== 批量修改分组 =====
const batchGroupVisible = ref(false)
const batchGroupTarget = ref<number | null>(null)

async function handleBatchGroup() {
  if (selectedIds.value.length === 0) return
  try {
    await batchUpdateSuiteGroup(projectId.value, { suiteIds: selectedIds.value, groupId: batchGroupTarget.value })
    ElMessage.success('批量修改分组成功')
    batchGroupVisible.value = false
    batchGroupTarget.value = null
    clearSelection()
    fetchGroups()
    fetchList()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  }
}

// ===== 新建/编辑套件弹窗 =====
const modalVisible = ref(false)
const editingId = ref<number>(0)
const form = reactive({ name: '', description: '', groupId: null as number | null })

function openCreate() {
  editingId.value = 0
  Object.assign(form, { name: '', description: '', groupId: activeGroupId.value > 0 ? activeGroupId.value : null })
  modalVisible.value = true
}

async function handleSubmit() {
  if (!form.name) { ElMessage.warning('请输入套件名称'); return }
  try {
    if (editingId.value) {
      await updateSuite(projectId.value, editingId.value, { ...form })
      ElMessage.success('更新成功')
    } else {
      await createSuite(projectId.value, { ...form })
      ElMessage.success('创建成功')
    }
    modalVisible.value = false
    fetchGroups()
    fetchList()
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '操作失败') }
}

function handleDelete(record: any) {
  ElMessageBox.confirm(`确定删除套件「${record.name}」？其下所有用例将一并删除。`, '确认删除', { type: 'warning' })
    .then(async () => { await deleteSuite(projectId.value, record.id); ElMessage.success('删除成功'); fetchGroups(); fetchList() })
    .catch(() => {})
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
    openGroupModal('create', contextGroup.value.id)
  } else {
    openGroupModal('create')
  }
  closeContextMenu()
}

function contextCreateChild() {
  if (contextGroup.value) openGroupModal('create', contextGroup.value.id)
  closeContextMenu()
}

function contextEdit() {
  if (contextGroup.value) openGroupModal('edit', undefined, contextGroup.value)
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
      ? '确定清空项目下的所有套件？其下所有用例将一并删除，此操作不可恢复。'
      : isUngrouped
        ? '确定清空「未分组」中的所有套件？其下所有用例将一并删除，此操作不可恢复。'
        : `确定清空分组「${g.name}」及其子分组中的所有套件？其下所有用例将一并删除，此操作不可恢复。`,
    '确认清空',
    { type: 'warning', confirmButtonText: '清空', cancelButtonText: '取消' }
  )
    .then(async () => {
      if (isAll) {
        await clearProjectSuites(projectId.value)
      } else {
        // 未分组（-1）后端语义为 groupId=0
        await clearGroupSuites(projectId.value, isUngrouped ? 0 : g.id)
      }
      ElMessage.success('已清空')
      fetchGroups()
      fetchList()
    })
    .catch(() => {})
}

// ===== 分组新建/编辑弹窗 =====
const groupModalVisible = ref(false)
const groupModalMode = ref<'create' | 'edit'>('create')
const editingGroupId = ref<number>(0)
const groupForm = reactive({ name: '', description: '', parentId: null as number | null })

function openGroupModal(mode: 'create' | 'edit', parentId?: number, group?: any) {
  groupModalMode.value = mode
  if (mode === 'create') {
    editingGroupId.value = 0
    Object.assign(groupForm, { name: '', description: '', parentId: parentId ?? null })
  } else {
    editingGroupId.value = group.id
    Object.assign(groupForm, { name: group.name, description: group.description || '', parentId: group.parentId ?? null })
  }
  groupModalVisible.value = true
}

async function handleGroupSubmit() {
  if (!groupForm.name) { ElMessage.warning('请输入分组名称'); return }
  try {
    if (groupModalMode.value === 'create') {
      await createSuiteGroup(projectId.value, { name: groupForm.name, description: groupForm.description, parentId: groupForm.parentId })
      ElMessage.success('分组创建成功')
    } else {
      await updateSuiteGroup(projectId.value, editingGroupId.value, { name: groupForm.name, description: groupForm.description, parentId: groupForm.parentId })
      ElMessage.success('分组更新成功')
    }
    groupModalVisible.value = false
    fetchGroups()
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '操作失败') }
}

async function handleDeleteGroup(group: any) {
  try {
    await ElMessageBox.confirm(`确定删除分组「${group.name}」吗？删除后，该分组下的套件将自动归入「未分组」。`, '删除分组', { type: 'warning' })
    await deleteSuiteGroup(projectId.value, group.id)
    ElMessage.success('分组删除成功')
    if (activeGroupId.value === group.id) activeGroupId.value = 0
    fetchGroups()
    fetchList()
  } catch { /* cancelled */ }
}

// ===== 通过率标签 =====
function passRateTag(rate: number | undefined) {
  if (rate === undefined || rate === -1) return { label: '--', type: 'info' as const }
  if (rate >= 90) return { label: `${rate}%`, type: 'success' as const }
  if (rate >= 70) return { label: `${rate}%`, type: 'warning' as const }
  return { label: `${rate}%`, type: 'danger' as const }
}

// ===== 用户分组，用于套件新建/编辑与批量修改分组弹窗 =====
const userGroups = computed(() => groups.value)

onMounted(() => {
  fetchGroups()
  fetchList()
  document.addEventListener('click', onDocClick)
})
onBeforeUnmount(() => {
  document.removeEventListener('click', onDocClick)
})
function onDocClick() { closeContextMenu() }
</script>

<template>
  <div>
    <PageHeader title="测试套件">
      <el-button v-if="hasPermission('project:suite:add')" type="primary" @click="openCreate">+ 新建套件</el-button>
    </PageHeader>

    <div class="suite-layout">
      <!-- 左侧分组树 -->
      <div class="group-panel" @contextmenu="handleBlankContextmenu">
        <div class="group-head">
          <span class="group-title">分组</span>
        </div>
        <div class="tree-search">
          <el-input v-model="filterText" size="small" placeholder="搜索分组..." clearable prefix-icon="Search" @input="(v: string) => treeRef?.filter(v)" />
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
                <span class="group-count">{{ data.suiteCount ?? 0 }}</span>
                <span v-if="data.isSystem === 1" class="group-lock" title="系统默认分组">🔒</span>
              </div>
            </template>
          </el-tree>
        </div>
      </div>

      <!-- 右侧内容 -->
      <div class="suite-content">
        <ProSearchCard :loading="loading" @search="handleSearch" @reset="handleReset">
          <div class="pro-search-field">
            <span class="pro-search-label">套件名称</span>
            <el-input v-model="search.name" placeholder="搜索套件名称" clearable style="width: 180px" @keyup.enter="handleSearch" />
          </div>
          <div class="pro-search-field">
            <span class="pro-search-label">优先级</span>
            <el-select v-model="search.priority" placeholder="全部" clearable style="width: 100px">
              <el-option v-for="p in priorityOptions" :key="p.value" :value="p.value" :label="p.label" />
            </el-select>
          </div>
        </ProSearchCard>

        <BatchBar
          :selected-count="selectedIds.length"
          :actions="[
            { key: 'changeGroup', label: '批量修改分组' },
            { key: 'delete', label: '批量删除', danger: true },
          ]"
          @action="handleBatchAction"
          @clear="clearSelection"
        />

        <el-table :data="list" v-loading="loading" border stripe style="width: 100%" @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="45" />
          <el-table-column prop="name" label="套件名称" min-width="200">
            <template #default="{ row }">
              <div class="suite-name-cell">
                <span class="suite-name suite-name-link" @click="router.push(`/project/${projectId}/suites/${row.id}/edit`)">{{ row.name }}</span>
                <div v-if="row.description" class="suite-desc">{{ row.description }}</div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="caseCount" label="用例数" width="90" align="center" />
          <el-table-column label="分组" width="120">
            <template #default="{ row }">
              <el-tag size="small" type="info">{{ row.groupId ? (groupMap[row.groupId]?.name || '未知') : '未分组' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="最近执行通过率" width="140" align="center">
            <template #default="{ row }">
              <el-tag :type="passRateTag(passRateMap[row.id]).type" size="small">{{ passRateTag(passRateMap[row.id]).label }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="router.push(`/project/${projectId}/cases?suiteId=${row.id}`)">查看用例</el-button>
              <el-button v-if="hasPermission('project:suite:edit')" type="primary" link size="small" @click="router.push(`/project/${projectId}/suites/${row.id}/edit`)">编辑</el-button>
              <el-button v-if="hasPermission('project:suite:delete')" type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
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

    <!-- 新建/编辑套件弹窗 -->
    <el-dialog v-model="modalVisible" :title="editingId ? '编辑套件' : '新建套件'" width="480px">
      <el-form label-position="top">
        <el-form-item label="套件名称" required>
          <el-input v-model="form.name" placeholder="例如：冒烟测试套件" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="套件描述" />
        </el-form-item>
        <el-form-item label="所属分组">
          <el-tree-select
            v-model="form.groupId"
            :data="[{ id: null, name: '未分组', children: [] }, ...userGroups.map((g: any) => ({ id: g.id, name: g.name, parentId: g.parentId }))]"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            check-strictly
            clearable
            placeholder="选择分组"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="modalVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 批量修改分组弹窗 -->
    <el-dialog v-model="batchGroupVisible" title="批量修改分组" width="420px">
      <p style="margin: 0 0 12px; color: #606266; font-size: 13px;">
        将选中的 <b style="color: #409eff">{{ selectedIds.length }}</b> 个套件的分组修改为：
      </p>
      <el-tree-select
        v-model="batchGroupTarget"
        :data="[{ id: null, name: '未分组', children: [] }, ...userGroups.map((g: any) => ({ id: g.id, name: g.name, parentId: g.parentId }))]"
        :props="{ label: 'name', value: 'id', children: 'children' }"
        check-strictly
        clearable
        placeholder="选择目标分组"
        style="width: 100%"
      />
      <template #footer>
        <el-button @click="batchGroupVisible = false">取消</el-button>
        <el-button type="primary" @click="handleBatchGroup">确认修改</el-button>
      </template>
    </el-dialog>

    <!-- 分组新建/编辑弹窗 -->
    <el-dialog v-model="groupModalVisible" :title="groupModalMode === 'edit' ? '编辑分组' : (groupForm.parentId ? '新建子分组' : '新建分组')" width="420px">
      <el-form label-position="top">
        <el-form-item label="分组名称" required>
          <el-input v-model="groupForm.name" placeholder="请输入分组名称" />
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
          <div v-if="hasPermission('project:suite:group')" class="context-menu-item" @click="contextCreateGroup">新建分组</div>
        </template>
        <!-- 系统分组（全部/未分组）右键：仅允许清空 -->
        <template v-else-if="contextGroup.isSystem === 1">
          <div class="context-menu-item danger" @click="contextClear">清空套件</div>
        </template>
        <!-- 用户分组右键 -->
        <template v-else>
          <div v-if="hasPermission('project:suite:group')" class="context-menu-item" @click="contextCreateChild">新建子分组</div>
          <div v-if="hasPermission('project:suite:group')" class="context-menu-divider" />
          <div class="context-menu-item" @click="contextEdit">编辑</div>
          <div class="context-menu-item danger" @click="contextClear">清空套件</div>
          <div class="context-menu-item danger" @click="contextDelete">删除</div>
        </template>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.suite-layout {
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
.suite-content {
  flex: 1;
  min-width: 0;
}
.suite-name-cell {
  line-height: 1.5;
}
.suite-name {
  font-weight: 500;
}
.suite-name-link {
  cursor: pointer;
  color: #409eff;
  transition: color 0.15s;
}
.suite-name-link:hover {
  color: #66b1ff;
  text-decoration: underline;
}
.suite-desc {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
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
</style>
