<!--
 @author HXN
 @date 2026-08-20 15:34
 @description Action 关键字列表视图
-->
<script setup lang="ts">
/**
 * Action 关键字列表 - M7
 * 左侧分组面板 + 右侧高级搜索 + 批量操作 + 表字段调整 + 智能分页
 * 对齐原型 action-list.html
 */
import { ref, reactive, onMounted, onBeforeUnmount, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getActions, deleteAction,
  getActionGroups, createActionGroup, updateActionGroup, deleteActionGroup, batchMoveActions,
  clearActionGroupActions, clearAllActions,
} from '@/api/action'
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

// ===== 列表数据 =====
const loading = ref(false)
const list = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })
const selectedRows = ref<any[]>([])

// ===== 搜索条件 =====
const search = reactive({ name: '', desc: '' })

// ===== 分组 =====
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
  groups.value.filter((g) => g.isSystem !== 1 || g.name === '未分组')
)
// 分组树：全部(虚拟) + 系统分组(未分组等，排除全部) + 用户分组按 parentId 建树
const groupTree = computed(() => {
  const userGrps = groups.value.filter((g) => g.isSystem !== 1)
  const buildTree = (parentId: number | null): any[] =>
    userGrps
      .filter((g) => (g.parentId ?? null) === parentId)
      .map((g) => ({ ...g, children: buildTree(g.id) }))
  const systemGroups = groups.value
    .filter((g) => g.isSystem === 1 && g.name !== '全部')
    .map((g) => ({ ...g, children: [] }))
  return [
    { id: 0, name: '全部', isSystem: 1, actionCount: pagination.total, children: [] },
    ...systemGroups,
    ...buildTree(null),
  ]
})

const treeRef = ref()
function filterNode(value: string, data: any) {
  if (!value) return true
  return (data.name || '').includes(value)
}

function onGroupNodeClick(data: any) {
  selectGroup(data.id)
}

async function fetchGroups() {
  if (!projectId.value) return
  try {
    const res: any = await getActionGroups(projectId.value)
    groups.value = res.data || []
  } catch { groups.value = [] }
}

function selectGroup(id: number) {
  activeGroupId.value = id === activeGroupId.value ? 0 : id
  pagination.current = 1
  fetchList()
}

async function fetchList() {
  if (!projectId.value) return
  loading.value = true
  try {
    const res: any = await getActions(projectId.value, {
      keyword: search.name || undefined,
      groupId: activeGroupId.value || undefined,
      page: pagination.current,
      pageSize: pagination.pageSize,
    })
    let items = res.data?.items || []
    if (search.desc) {
      items = items.filter((item: any) =>
        (item.description || '').toLowerCase().includes(search.desc.toLowerCase()),
      )
    }
    list.value = items
    pagination.total = res.data?.total || 0
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  fetchList()
}

function handleReset() {
  Object.assign(search, { name: '', desc: '' })
  handleSearch()
}

// ===== 新建 =====
function handleCreate() {
  // 当前选中用户分组时带入作为新建页默认分组（"全部"/"未分组"等系统分组不带入）
  const g = groupMap.value[activeGroupId.value]
  const groupId = g && g.isSystem !== 1 ? activeGroupId.value : undefined
  router.push({
    path: `/project/${projectId.value}/actions/new`,
    query: groupId ? { groupId: String(groupId) } : undefined,
  })
}

// ===== 删除 =====
function handleDelete(record: any) {
  ElMessageBox.confirm(
    `确定删除 Action 关键字「${record.name}」？删除后将无法恢复。`,
    '删除 Action关键字',
    { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' },
  )
    .then(async () => {
      await deleteAction(projectId.value, record.id)
      ElMessage.success('已删除 1 条 Action关键字')
      fetchGroups()
      fetchList()
    })
    .catch(() => {})
}

// ===== 右键菜单 =====
const contextMenuVisible = ref(false)
const contextMenuPos = reactive({ x: 0, y: 0 })
const contextGroup = ref<any>(null)

function handleNodeContextmenu(e: MouseEvent, data: any) {
  e.preventDefault()
  e.stopPropagation()
  // 系统分组仅允许"全部"(id=0)和"未分组"显示清空菜单
  if (data.isSystem === 1 && data.id !== 0 && data.name !== '未分组') return
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
  ElMessageBox.confirm(
    isAll
      ? '确定清空项目下的所有 Action 关键字？此操作不可恢复。'
      : `确定清空分组「${g.name}」及其子分组中的所有 Action 关键字？此操作不可恢复。`,
    '确认清空',
    { type: 'warning', confirmButtonText: '清空', cancelButtonText: '取消' },
  )
    .then(async () => {
      if (isAll) {
        await clearAllActions(projectId.value)
      } else {
        await clearActionGroupActions(projectId.value, g.id)
      }
      ElMessage.success('已清空')
      fetchGroups()
      fetchList()
    })
    .catch(() => {})
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
      await updateActionGroup(projectId.value, editingGroupId.value, groupForm)
      ElMessage.success('更新成功')
    } else {
      await createActionGroup(projectId.value, groupForm)
      ElMessage.success('创建成功')
    }
    groupModalVisible.value = false
    fetchGroups()
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '操作失败') }
}
function handleDeleteGroup(g: any) {
  if (g.isSystem === 1) { ElMessage.info('系统分组不可删除'); return }
  ElMessageBox.confirm(`确定删除分组「${g.name}」？`, '确认删除', { type: 'warning' })
    .then(async () => { await deleteActionGroup(projectId.value, g.id); ElMessage.success('删除成功'); fetchGroups() })
    .catch(() => {})
}

// ===== 批量操作 =====
const selectedIds = computed(() => selectedRows.value.map((r: any) => r.id))
function handleSelectionChange(rows: any[]) { selectedRows.value = rows }

function handleBatchAction(key: string) {
  if (key === 'delete') handleBatchDelete()
  else if (key === 'move') batchMoveVisible.value = true
}
function clearSelection() { selectedRows.value = [] }

// ===== 批量删除 =====
function handleBatchDelete() {
  const count = selectedIds.value.length
  const preview = selectedRows.value.map((r: any) => r.name).join('、')
  ElMessageBox.confirm(
    `确定删除已选中的 ${count} 条 Action关键字吗？\n${preview}\n删除后将无法恢复，请谨慎操作。`,
    '批量删除 Action关键字',
    { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' },
  )
    .then(async () => {
      for (const id of selectedIds.value) {
        await deleteAction(projectId.value, id)
      }
      ElMessage.success(`已删除 ${count} 条 Action关键字`)
      clearSelection()
      fetchGroups()
      fetchList()
    })
    .catch(() => {})
}

// 批量改组
const batchMoveVisible = ref(false)
const batchMoveTarget = ref<number | null>(null)
const batchMovePreview = computed(() => {
  const target = groups.value.find((g: any) => g.id === batchMoveTarget.value)
  return selectedRows.value.map((r: any) => ({
    name: r.name,
    oldGroup: groupMap.value[r.groupId]?.name || '未分组',
    newGroup: target?.name || '',
  }))
})
async function handleBatchMove() {
  if (!batchMoveTarget.value && batchMoveTarget.value !== 0) { ElMessage.warning('请选择目标分组'); return }
  try {
    await batchMoveActions(projectId.value, batchMoveTarget.value, selectedIds.value)
    ElMessage.success('移动成功')
    batchMoveVisible.value = false
    batchMoveTarget.value = null
    clearSelection()
    fetchGroups()
    fetchList()
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '操作失败') }
}

// ===== 表字段调整 =====
const defaultColumns: ColumnItem[] = [
  { key: 'name', label: '名称', locked: true, visible: true },
  { key: 'group', label: '分组', locked: false, visible: true },
  { key: 'desc', label: '描述', locked: true, visible: true },
  { key: 'refs', label: '引用次数', locked: false, visible: false },
  { key: 'createTime', label: '创建时间', locked: false, visible: true },
  { key: 'updateTime', label: '更新时间', locked: false, visible: false },
  { key: 'action', label: '操作', locked: true, visible: true },
]
const columns = ref<ColumnItem[]>(defaultColumns.map((c) => ({ ...c })))

function isColVisible(key: string) {
  return columns.value.find((c) => c.key === key)?.visible ?? false
}

function resetColumns() {
  columns.value = defaultColumns.map((c) => ({ ...c }))
}

// ===== 生命周期 =====
function onDocClick() { closeContextMenu() }
onMounted(() => {
  fetchGroups()
  fetchList()
  document.addEventListener('click', onDocClick)
})
onBeforeUnmount(() => {
  document.removeEventListener('click', onDocClick)
})
</script>

<template>
  <div>
    <PageHeader title="Action关键字">
      <el-button
        v-if="hasPermission('project:action:add')"
        type="primary"
        @click="handleCreate"
      >
        + 新建 Action关键字
      </el-button>
    </PageHeader>

    <div class="action-layout">
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
            :data="groupTree"
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
                <span class="group-count">{{ data.actionCount ?? 0 }}</span>
                <span v-if="data.isSystem === 1" class="group-lock" title="系统默认分组">🔒</span>
              </div>
            </template>
          </el-tree>
        </div>
      </div>

      <!-- 右侧内容 -->
      <div class="action-content">
        <ProSearchCard :loading="loading" @search="handleSearch" @reset="handleReset">
          <div class="pro-search-field">
            <span class="pro-search-label">名称</span>
            <el-input
              v-model="search.name"
              placeholder="输入名称"
              clearable
              style="width: 180px"
              @keyup.enter="handleSearch"
            />
          </div>
          <div class="pro-search-field">
            <span class="pro-search-label">描述</span>
            <el-input
              v-model="search.desc"
              placeholder="输入描述"
              clearable
              style="width: 180px"
              @keyup.enter="handleSearch"
            />
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
          v-if="hasPermission('project:action:delete')"
          :selected-count="selectedIds.length"
          :actions="[
            { key: 'move', label: '批量修改分组' },
            { key: 'delete', label: '批量删除', danger: true },
          ]"
          @action="handleBatchAction"
          @clear="clearSelection"
        />

        <el-table
          :data="list"
          v-loading="loading"
          border
          stripe
          style="width: 100%"
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="45" />
          <el-table-column
            v-if="isColVisible('name')"
            prop="name"
            label="名称"
            width="220"
            show-overflow-tooltip
          />
          <el-table-column
            v-if="isColVisible('group')"
            label="分组"
            width="140"
            show-overflow-tooltip
          >
            <template #default="{ row }">
              <span v-if="row.groupId">{{ groupMap[row.groupId]?.name || '未知分组' }}</span>
              <span v-else style="color: #c0c4cc">未分组</span>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isColVisible('desc')"
            prop="description"
            label="描述"
            show-overflow-tooltip
          >
            <template #default="{ row }">
              <span v-if="row.description">{{ row.description }}</span>
              <span v-else style="color: #c0c4cc">--</span>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isColVisible('refs')"
            label="引用次数"
            width="100"
            align="center"
          >
            <template #default="{ row }">{{ row.referenceCount ?? 0 }}</template>
          </el-table-column>
          <el-table-column
            v-if="isColVisible('createTime')"
            label="创建时间"
            width="160"
          >
            <template #default="{ row }">{{
              row.createdAt?.substring(0, 16)?.replace('T', ' ')
            }}</template>
          </el-table-column>
          <el-table-column
            v-if="isColVisible('updateTime')"
            label="更新时间"
            width="160"
          >
            <template #default="{ row }">{{
              row.updatedAt?.substring(0, 16)?.replace('T', ' ')
            }}</template>
          </el-table-column>
          <el-table-column
            v-if="isColVisible('action')"
            label="操作"
            width="180"
            fixed="right"
          >
            <template #default="{ row }">
              <el-button
                v-if="hasPermission('project:action:edit')"
                type="primary"
                link
                size="small"
                @click="router.push(`/project/${projectId}/actions/${row.id}/edit`)"
              >
                编辑
              </el-button>
              <el-button
                v-if="hasPermission('project:action:debug')"
                type="primary"
                link
                size="small"
                @click="router.push(`/project/${projectId}/actions/${row.id}/debug`)"
              >
                调试
              </el-button>
              <el-button
                v-if="hasPermission('project:action:delete')"
                type="danger"
                link
                size="small"
                @click="handleDelete(row)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
          <template #empty>
            <div style="padding: 48px 20px; color: #909399">
              <div>暂无数据</div>
            </div>
          </template>
        </el-table>

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
          <el-input v-model="groupForm.name" placeholder="如：用户管理服务" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="groupForm.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="groupModalVisible = false">取消</el-button>
        <el-button type="primary" @click="handleGroupSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 批量修改分组弹窗 -->
    <el-dialog v-model="batchMoveVisible" title="批量修改分组" width="480px">
      <p style="margin: 0 0 12px; color: #606266; font-size: 13px;">
        将选中的 <b style="color: #409eff">{{ selectedIds.length }}</b> 条 Action 的分组修改为：
      </p>
      <el-select v-model="batchMoveTarget" placeholder="选择目标分组" filterable style="width: 100%; margin-bottom: 12px">
        <el-option
          v-for="g in moveTargetGroups"
          :key="g.id" :value="g.id" :label="g.name"
        />
      </el-select>
      <div v-if="batchMovePreview.length" class="batch-preview">
        <div class="batch-preview-title">以下 Action 的分组将被修改：</div>
        <div class="batch-preview-list">
          <div v-for="item in batchMovePreview" :key="item.name" class="batch-preview-item">
            <span class="batch-preview-name">{{ item.name }}</span>
            <span class="batch-preview-old">{{ item.oldGroup }}</span>
            <span class="batch-preview-arrow">→</span>
            <span :class="['batch-preview-new', { empty: !item.newGroup }]">{{ item.newGroup || '请选择目标分组' }}</span>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="batchMoveVisible = false">取消</el-button>
        <el-button type="primary" @click="handleBatchMove">确认修改</el-button>
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
.action-layout {
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
.action-content {
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

/* 批量操作预览列表 */
.batch-preview {
  margin-top: 8px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 13px;
}
.batch-preview-title {
  color: #909399;
  margin-bottom: 6px;
}
.batch-preview-list {
  max-height: 160px;
  overflow-y: auto;
}
.batch-preview-item {
  padding: 3px 0;
  display: flex;
  align-items: center;
  gap: 6px;
}
.batch-preview-name {
  color: #606266;
}
.batch-preview-old {
  color: #909399;
  font-size: 12px;
}
.batch-preview-arrow {
  color: #909399;
  font-size: 12px;
}
.batch-preview-new {
  color: #409eff;
  font-weight: 500;
}
.batch-preview-new.empty {
  color: #c0c4cc;
  font-style: italic;
  font-weight: normal;
}
</style>
