<!--
 @author HXN
 @date 2026-08-30
 @description 手动化用例列表视图
-->
<script setup lang="ts">
/**
 * 手动化用例列表
 * 左侧分组树 + 右侧高级搜索 + 批量操作 + 分页表格
 */
import { ref, reactive, onMounted, onBeforeUnmount, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getManualCases, deleteManualCase, toggleManualCaseStatus,
  getManualCaseGroups, createManualCaseGroup, updateManualCaseGroup,
  deleteManualCaseGroup, clearManualGroupCases, clearManualProjectCases
} from '@/api/manualCase'
import PageHeader from '@/components/PageHeader/index.vue'
import ProSearchCard from '@/components/ProSearchCard/index.vue'
import BatchBar from '@/components/BatchBar/index.vue'
import ProPagination from '@/components/ProPagination/index.vue'
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

// ===== 搜索条件 =====
const search = reactive({ title: '', priority: '', caseType: '', caseStatus: '' })

// ===== 分组 =====
const groups = ref<any[]>([])
const activeGroupId = ref<number>(0)
const filterText = ref('')
const userGroups = computed(() => groups.value.filter((g) => g.isSystem !== 1))
const ungroupedCount = computed(() => {
  const topUserGroups = groups.value.filter((g) => g.isSystem !== 1 && (g.parentId ?? null) === null)
  const groupedSum = topUserGroups.reduce((acc, g) => acc + (g.caseCount || 0), 0)
  return Math.max(0, pagination.total - groupedSum)
})
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
    const res: any = await getManualCaseGroups(projectId.value)
    groups.value = res.data || []
  } catch { groups.value = [] }
}

async function fetchList() {
  loading.value = true
  try {
    const groupIdParam = activeGroupId.value === 0 ? undefined : activeGroupId.value === -1 ? 0 : activeGroupId.value
    const res: any = await getManualCases(projectId.value, {
      groupId: groupIdParam,
      keyword: search.title || undefined,
      priority: search.priority || undefined,
      caseType: search.caseType || undefined,
      caseStatus: search.caseStatus || undefined,
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
  Object.assign(search, { title: '', priority: '', caseType: '', caseStatus: '' })
  handleSearch()
}

// ===== 右键菜单 =====
const contextMenuVisible = ref(false)
const contextMenuPos = reactive({ x: 0, y: 0 })
const contextGroup = ref<any>(null)

function handleNodeContextmenu(e: MouseEvent, data: any) {
  e.preventDefault()
  e.stopPropagation()
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
  if (contextGroup.value) openCreateGroup(contextGroup.value.id)
  else openCreateGroup()
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
      ? '确定清空项目下的所有手动化用例？此操作不可恢复。'
      : isUngrouped
        ? '确定清空「未分组」中的所有手动化用例？此操作不可恢复。'
        : `确定清空分组「${g.name}」及其子分组中的所有手动化用例？此操作不可恢复。`,
    '确认清空',
    { type: 'warning', confirmButtonText: '清空', cancelButtonText: '取消' }
  )
    .then(async () => {
      if (isAll) {
        await clearManualProjectCases(projectId.value)
      } else {
        await clearManualGroupCases(projectId.value, isUngrouped ? 0 : g.id)
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
    `确定删除选中的 ${selectedIds.value.length} 条用例？`,
    '批量删除', { type: 'warning' },
  ).then(async () => {
    for (const id of selectedIds.value) {
      await deleteManualCase(projectId.value, id)
    }
    ElMessage.success('删除成功')
    clearSelection()
    fetchGroups(); fetchList()
  }).catch(() => {})
}

async function handleBatchToggle(enable: boolean) {
  const target = enable ? '启用' : '废弃'
  try {
    for (const row of selectedRows.value) {
      const shouldToggle = enable ? row.caseStatus !== 1 : row.caseStatus === 1
      if (shouldToggle) {
        await toggleManualCaseStatus(projectId.value, row.id)
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
    const { updateManualCase } = await import('@/api/manualCase')
    for (const id of selectedIds.value) {
      await updateManualCase(projectId.value, id, { groupId: batchMoveTarget.value })
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
  router.push(`/project/${projectId.value}/manual-cases/new`)
}

function handleEdit(record: any) {
  router.push(`/project/${projectId.value}/manual-cases/${record.id}/edit`)
}

async function handleToggleStatus(record: any) {
  try {
    await toggleManualCaseStatus(projectId.value, record.id)
    ElMessage.success(record.caseStatus === 1 ? '已废弃' : '已启用')
    fetchList()
  } catch { ElMessage.error('操作失败') }
}

function handleDelete(record: any) {
  ElMessageBox.confirm(`确定删除用例「${record.title}」？`, '确认删除', { type: 'warning' })
    .then(async () => { await deleteManualCase(projectId.value, record.id); ElMessage.success('删除成功'); fetchGroups(); fetchList() })
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
      await updateManualCaseGroup(projectId.value, editingGroupId.value, groupForm)
      ElMessage.success('更新成功')
    } else {
      await createManualCaseGroup(projectId.value, groupForm)
      ElMessage.success('创建成功')
    }
    groupModalVisible.value = false
    fetchGroups()
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '操作失败') }
}
function handleDeleteGroup(g: any) {
  if (g.isSystem === 1) { ElMessage.info('系统分组不可删除'); return }
  ElMessageBox.confirm(`确定删除分组「${g.name}」？删除后该分组下的用例将自动归入「未分组」。`, '确认删除', { type: 'warning' })
    .then(async () => { await deleteManualCaseGroup(projectId.value, g.id); ElMessage.success('删除成功'); fetchGroups() })
    .catch(() => {})
}

// ===== 常量映射 =====
const priorityTypeMap: Record<string, string> = { '高': 'danger', '中': 'warning', '低': 'info' }
const caseTypeLabel: Record<string, string> = { NORMAL: '正常', EXCEPTION: '异常' }
const caseTypeTagType: Record<string, string> = { NORMAL: 'success', EXCEPTION: 'warning' }

// ===== 生命周期 =====
const treeRef = ref()
function onDocClick() { closeContextMenu() }
onMounted(() => {
  fetchGroups(); fetchList()
  document.addEventListener('click', onDocClick)
})
onBeforeUnmount(() => {
  document.removeEventListener('click', onDocClick)
})
</script>

<template>
  <div>
    <PageHeader title="手动化用例">
      <el-button v-if="hasPermission('project:manual-case:add')" type="primary" @click="openCreate">+ 新建用例</el-button>
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
            <span class="pro-search-label">用例标题</span>
            <el-input v-model="search.title" placeholder="搜索用例标题" clearable style="width: 180px" @keyup.enter="handleSearch" />
          </div>
          <div class="pro-search-field">
            <span class="pro-search-label">优先级</span>
            <el-select v-model="search.priority" placeholder="全部" clearable style="width: 100px">
              <el-option v-for="p in priorityOptions" :key="p.value" :value="p.value" :label="p.label" />
            </el-select>
          </div>
          <div class="pro-search-field">
            <span class="pro-search-label">用例类型</span>
            <el-select v-model="search.caseType" placeholder="全部" clearable style="width: 100px">
              <el-option value="NORMAL" label="正常" />
              <el-option value="EXCEPTION" label="异常" />
            </el-select>
          </div>
          <template #collapse>
            <div class="pro-search-field">
              <span class="pro-search-label">用例状态</span>
              <el-select v-model="search.caseStatus" placeholder="全部状态" clearable style="width: 120px">
                <el-option value="1" label="使用" />
                <el-option value="0" label="废弃" />
              </el-select>
            </div>
          </template>
        </ProSearchCard>

        <BatchBar
          :selected-count="selectedIds.length"
          :actions="[
            { key: 'enable', label: '批量启用' },
            { key: 'disable', label: '批量废弃' },
            { key: 'move', label: '批量修改分组' },
            { key: 'delete', label: '批量删除', danger: true },
          ]"
          @action="handleBatchAction"
          @clear="clearSelection"
        />

        <el-table :data="list" v-loading="loading" border stripe style="width: 100%" @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="45" />
          <el-table-column prop="title" label="用例标题" min-width="200" show-overflow-tooltip>
            <template #default="{ row }">
              <el-button type="primary" link @click="handleEdit(row)">{{ row.title }}</el-button>
            </template>
          </el-table-column>
          <el-table-column label="用例类型" width="90">
            <template #default="{ row }">
              <el-tag :type="(caseTypeTagType[row.caseType] || 'info') as any" size="small">{{ caseTypeLabel[row.caseType] || row.caseType }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="优先级" width="80">
            <template #default="{ row }">
              <el-tag :type="(priorityTypeMap[row.priority] || 'info') as any" size="small">{{ row.priority }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="测试环境" width="90">
            <template #default="{ row }">
              <el-tag :type="row.runInTestEnv === 1 ? 'success' : 'info'" size="small">{{ row.runInTestEnv === 1 ? '是' : '否' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="生产环境" width="90">
            <template #default="{ row }">
              <el-tag :type="row.runInProdEnv === 1 ? 'success' : 'info'" size="small">{{ row.runInProdEnv === 1 ? '是' : '否' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.caseStatus === 1 ? 'success' : 'danger'" size="small">{{ row.caseStatus === 1 ? '使用' : '废弃' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button v-if="hasPermission('project:manual-case:edit')" type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button v-if="hasPermission('project:manual-case:toggle')" type="primary" link size="small" @click="handleToggleStatus(row)">{{ row.caseStatus === 1 ? '废弃' : '启用' }}</el-button>
              <el-button v-if="hasPermission('project:manual-case:delete')" type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
          <template #empty>
            <div style="padding:48px 20px;text-align:center;color:#909399">
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
          <el-input v-model="groupForm.name" placeholder="如：登录模块" />
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
        将选中的 <b style="color: #409eff">{{ selectedIds.length }}</b> 条用例移动到：
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
        <template v-if="!contextGroup">
          <div v-if="hasPermission('project:manual-case:group')" class="context-menu-item" @click="contextCreateGroup">新建分组</div>
        </template>
        <template v-else-if="contextGroup.isSystem === 1">
          <div class="context-menu-item danger" @click="contextClear">清空用例</div>
        </template>
        <template v-else>
          <div v-if="hasPermission('project:manual-case:group')" class="context-menu-item" @click="contextCreateChild">新建子分组</div>
          <div v-if="hasPermission('project:manual-case:group')" class="context-menu-divider" />
          <div class="context-menu-item" @click="contextEdit">编辑</div>
          <div class="context-menu-item danger" @click="contextClear">清空用例</div>
          <div class="context-menu-item danger" @click="contextDelete">删除</div>
        </template>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.case-layout { display: flex; gap: 16px; min-height: calc(100vh - 164px); }
.group-panel { width: 220px; flex-shrink: 0; background: #fff; border: 1px solid #ebeef5; border-radius: 6px; padding: 12px; display: flex; flex-direction: column; overflow: hidden; }
.group-head { display: flex; justify-content: space-between; align-items: center; }
.group-title { font-weight: 600; font-size: 14px; color: #303133; }
.tree-search { margin: 8px 0; }
.tree-search :deep(.el-input__wrapper) { box-shadow: 0 0 0 1px #dcdfe6 inset; border-radius: 4px; }
.group-tree { flex: 1; overflow-y: auto; }
.group-tree :deep(.el-tree-node__content) { height: auto; padding: 2px 0; }
.group-tree-node { display: flex; align-items: center; flex: 1; padding: 2px 4px; border-radius: 4px; font-size: 13px; gap: 6px; width: 100%; }
.group-tree-node:hover { background: #f5f7fa; }
.group-tree-node.active { background: #ecf5ff; color: #409eff; font-weight: 500; }
.group-name { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.group-count { font-size: 12px; color: #909399; flex-shrink: 0; }
.group-lock { font-size: 10px; color: #c0c4cc; flex-shrink: 0; margin-left: 2px; }
.case-content { flex: 1; min-width: 0; }

.context-menu { position: fixed; background: #fff; border: 1px solid #ebeef5; border-radius: 4px; box-shadow: 0 2px 12px rgba(0,0,0,0.1); padding: 4px 0; min-width: 130px; z-index: 9999; }
.context-menu-item { padding: 7px 14px; font-size: 13px; color: #303133; cursor: pointer; display: flex; align-items: center; gap: 8px; transition: background 0.15s; }
.context-menu-item:hover { background: #f5f7fa; }
.context-menu-item.danger { color: #f56c6c; }
.context-menu-item.danger:hover { background: #fef0f0; }
.context-menu-divider { height: 1px; background: #ebeef5; margin: 4px 0; }
</style>
