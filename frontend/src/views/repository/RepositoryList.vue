<!--
 @author HXN
 @date 2026-08-30
 @description 源代码列表视图
-->
<script setup lang="ts">
/**
 * 源代码 - Git 仓库登记与代码拉取
 * 左侧分组树 + 右侧仓库列表（对齐接口文档 ApiList 布局）
 */
import { ref, reactive, onMounted, onBeforeUnmount, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getRepositories,
  createRepository,
  updateRepository,
  deleteRepository,
  pullRepository,
  getPullLogs,
  copyRepository,
  getRepositoryBranches,
  getRepositoryGroups,
  createRepositoryGroup,
  updateRepositoryGroup,
  deleteRepositoryGroup,
  batchMoveRepositories,
  batchDeleteRepositories,
} from '@/api/repository'
import BatchBar from '@/components/BatchBar/index.vue'
import { useProjectStore } from '@/stores/modules/project'
import { usePermission } from '@/composables/usePermission'
import { useDict } from '@/composables/useDict'

const route = useRoute()
const { hasPermission } = usePermission()
const projectStore = useProjectStore()
const projectId = computed(() => Number(route.params.id))

// 拉取状态字典（禁止前端硬编码状态文案）
const { options: pullStatusOptions } = useDict('repository_pull_status')
const pullStatusLabel = computed(() => {
  const map: Record<string, string> = {}
  pullStatusOptions.value.forEach((o) => {
    map[o.value] = o.label
  })
  return (val: string | null | undefined) => (val ? map[val] || val : '-')
})

const loading = ref(false)
const list = ref<any[]>([])

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
// 项目仓库总数（「未分组」+ 根级用户分组仓库数之和，不随选中分组过滤变化）
const totalRepoCount = computed(() => {
  const userGroups = groups.value.filter((g) => g.isSystem !== 1)
  const rootUserGroupIds = new Set(userGroups.filter((g) => g.parentId == null).map((g) => g.id))
  let total = 0
  for (const g of groups.value) {
    if (g.isSystem === 1 && g.name === '未分组') {
      total += g.repoCount || 0
    } else if (g.isSystem !== 1 && rootUserGroupIds.has(g.id)) {
      total += g.repoCount || 0
    }
  }
  return total
})
// 分组树：全部(虚拟) + 系统分组(未分组，排除全部) + 用户分组按 parentId 建树
const groupTree = computed(() => {
  const userGroups = groups.value.filter((g) => g.isSystem !== 1)
  const buildTree = (parentId: number | null): any[] =>
    userGroups
      .filter((g) => (g.parentId ?? null) === parentId)
      .map((g) => ({ ...g, children: buildTree(g.id) }))
  const systemGroups = groups.value
    .filter((g) => g.isSystem === 1 && g.name !== '全部')
    .map((g) => ({ ...g, children: [] }))
  return [
    { id: 0, name: '全部', isSystem: 1, repoCount: totalRepoCount.value, children: [] },
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
// 树形组件过滤回调
function filterNode(value: string, data: any) {
  if (!value) return true
  return data.name.toLowerCase().includes(value.toLowerCase())
}

function onGroupNodeClick(data: any) {
  selectGroup(data.id)
}

async function fetchGroups() {
  try {
    const res: any = await getRepositoryGroups(projectId.value)
    groups.value = res.data || []
  } catch { groups.value = [] }
}

function selectGroup(id: number) {
  activeGroupId.value = id === activeGroupId.value ? 0 : id
  fetchList()
}

// ===== 分组拖拽 =====
function allowDrag(node: any) {
  // 系统分组不可拖拽（全部、未分组等）
  return node.data.isSystem !== 1
}
function allowDrop(_draggingNode: any, dropNode: any, dropType: string) {
  const target = dropNode.data
  // 不允许放入系统分组内部（全部、未分组等）
  if (dropType === 'inner' && target.isSystem === 1) return false
  return true
}
async function onNodeDrop(draggingNode: any, dropNode: any, dropType: string) {
  let parentId: number | null
  if (dropType === 'inner') {
    parentId = dropNode.data.id
  } else {
    // before / after → 与目标节点同级
    parentId = dropNode.data.parentId ?? null
  }
  try {
    await updateRepositoryGroup(projectId.value, draggingNode.data.id, { parentId })
    fetchGroups()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '移动失败')
    fetchGroups()
  }
}

// ===== 分组右键菜单 =====
const contextMenuVisible = ref(false)
const contextMenuPos = reactive({ x: 0, y: 0 })
const contextGroup = ref<any>(null)

function handleNodeContextmenu(e: MouseEvent, data: any) {
  e.preventDefault()
  e.stopPropagation()
  // 系统分组不提供右键菜单
  if (data.isSystem === 1) return
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
      await updateRepositoryGroup(projectId.value, editingGroupId.value, groupForm)
      ElMessage.success('更新成功')
    } else {
      await createRepositoryGroup(projectId.value, { ...groupForm, projectId: projectId.value })
      ElMessage.success('创建成功')
    }
    groupModalVisible.value = false
    fetchGroups()
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '操作失败') }
}
function handleDeleteGroup(g: any) {
  if (g.isSystem === 1) { ElMessage.info('系统分组不可删除'); return }
  ElMessageBox.confirm(`确定删除分组「${g.name}」？`, '确认删除', { type: 'warning' })
    .then(async () => { await deleteRepositoryGroup(projectId.value, g.id); ElMessage.success('删除成功'); fetchGroups() })
    .catch(() => {})
}

// ===== 批量操作 =====
const selectedRows = ref<any[]>([])
const selectedIds = computed(() => selectedRows.value.map((r: any) => r.id))
function handleSelectionChange(rows: any[]) { selectedRows.value = rows }

function handleBatchAction(key: string) {
  if (key === 'delete') handleBatchDelete()
  else if (key === 'move') batchMoveVisible.value = true
}
function clearSelection() { selectedRows.value = [] }

// ===== 批量删除 =====
const batchDeleteVisible = ref(false)
function handleBatchDelete() {
  batchDeleteVisible.value = true
}
async function confirmBatchDelete() {
  try {
    await batchDeleteRepositories(projectId.value, selectedIds.value)
    ElMessage.success('已删除 ' + selectedIds.value.length + ' 个仓库')
    batchDeleteVisible.value = false
    clearSelection()
    fetchGroups(); fetchList()
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '删除失败') }
}

// 批量移动分组
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
  if (!batchMoveTarget.value) { ElMessage.warning('请选择目标分组'); return }
  try {
    await batchMoveRepositories(projectId.value, batchMoveTarget.value, selectedIds.value)
    ElMessage.success('移动成功')
    batchMoveVisible.value = false
    batchMoveTarget.value = null
    clearSelection()
    fetchGroups(); fetchList()
  } catch (e: any) { ElMessage.error(e?.response?.data?.message || '操作失败') }
}

// 新建/编辑弹窗
const modalVisible = ref(false)
const isEdit = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()
const form = reactive({
  groupId: null as number | null,
  name: '',
  gitUrl: '',
  branch: '',
  authUsername: '',
  authPassword: '',
  description: '',
})

const rules = reactive<FormRules>({
  name: [
    { required: true, message: '请输入仓库名称', trigger: 'blur' },
    { max: 50, message: '仓库名称长度不能超过 50 个字符', trigger: 'blur' },
  ],
  gitUrl: [
    { required: true, message: '请输入 Git 地址', trigger: 'blur' },
    { max: 500, message: 'Git 地址长度不能超过 500 个字符', trigger: 'blur' },
  ],
  branch: [{ max: 100, message: '分支长度不能超过 100 个字符', trigger: 'blur' }],
})

// 分支下拉选项（由远程仓库 lsRemote 获取，可搜索、可手动输入）
const branchOptions = ref<string[]>([])
const branchLoading = ref(false)
const defaultBranch = ref('')

async function handleFetchBranches() {
  if (!form.gitUrl.trim()) {
    ElMessage.warning('请先填写 Git 地址')
    return
  }
  branchLoading.value = true
  try {
    const res: any = await getRepositoryBranches(projectId.value, {
      gitUrl: form.gitUrl.trim(),
      authUsername: form.authUsername || '',
      authPassword: form.authPassword || '',
      repositoryId: isEdit.value && editingId.value ? editingId.value : null,
    })
    branchOptions.value = res.data?.branches || []
    defaultBranch.value = res.data?.defaultBranch || ''
    ElMessage.success(`获取成功，共 ${branchOptions.value.length} 个分支`)
  } catch (e: any) {
    branchOptions.value = []
    defaultBranch.value = ''
    ElMessage.error(e?.response?.data?.message || '获取分支失败')
  } finally {
    branchLoading.value = false
  }
}

// 行级拉取 loading（防止同一仓库重复点击拉取）
const pullingIds = ref<number[]>([])

// 行级复制 loading（防止同一仓库重复点击复制）
const copyingIds = ref<number[]>([])

// 拉取记录抽屉
const logsDrawerVisible = ref(false)
const logsLoading = ref(false)
const logsList = ref<any[]>([])
const currentRepoName = ref('')

async function fetchList() {
  loading.value = true
  try {
    const res: any = await getRepositories(projectId.value, activeGroupId.value || undefined)
    list.value = res.data || []
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
}

// 当前选中分组下的仓库名称集合（用于表格分组列展示）
const ungroupedGroupId = computed(() => {
  const g = groups.value.find((item: any) => item.isSystem === 1 && item.name === '未分组')
  return g?.id ?? null
})

function groupName(groupId: number | null | undefined) {
  if (groupId == null) return '未分组'
  return groupMap.value[groupId]?.name || '未分组'
}

function openCreate() {
  isEdit.value = false
  editingId.value = null
  Object.assign(form, {
    groupId: ungroupedGroupId.value,
    name: '',
    gitUrl: '',
    branch: '',
    authUsername: '',
    authPassword: '',
    description: '',
  })
  branchOptions.value = []
  defaultBranch.value = ''
  modalVisible.value = true
}

function openEdit(record: any) {
  isEdit.value = true
  editingId.value = record.id
  Object.assign(form, {
    groupId: record.groupId ?? ungroupedGroupId.value,
    name: record.name,
    gitUrl: record.gitUrl,
    branch: record.branch || '',
    authUsername: record.authUsername || '',
    authPassword: '',
    description: record.description || '',
  })
  branchOptions.value = []
  defaultBranch.value = ''
  modalVisible.value = true
}

function handleSubmit() {
  formRef.value?.validate(async (valid) => {
    if (!valid) return
    try {
      if (isEdit.value && editingId.value) {
        await updateRepository(projectId.value, editingId.value, { ...form })
        ElMessage.success('保存成功')
      } else {
        await createRepository(projectId.value, { ...form, projectId: projectId.value })
        ElMessage.success('创建成功')
      }
      modalVisible.value = false
      fetchGroups(); fetchList()
    } catch (e: any) {
      ElMessage.error(e?.response?.data?.message || '保存失败')
    }
  })
}

function handleDialogClosed() {
  formRef.value?.resetFields()
}

async function handlePull(record: any) {
  pullingIds.value.push(record.id)
  try {
    const res: any = await pullRepository(projectId.value, record.id)
    if (res.data?.success) {
      const typeText = res.data.pullType === 'CLONE' ? '克隆成功' : '拉取成功'
      const commitText = res.data.commitId ? `（${res.data.commitId.substring(0, 8)}）` : ''
      ElMessage.success(`${typeText}${commitText}`)
    } else {
      ElMessage.error(res.data?.message || '拉取失败')
    }
    fetchList()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '拉取失败')
  } finally {
    pullingIds.value = pullingIds.value.filter((id) => id !== record.id)
  }
}

async function handleShowLogs(record: any) {
  currentRepoName.value = record.name
  logsDrawerVisible.value = true
  logsLoading.value = true
  try {
    const res: any = await getPullLogs(projectId.value, record.id)
    logsList.value = res.data || []
  } catch {
    logsList.value = []
  } finally {
    logsLoading.value = false
  }
}

async function handleCopy(record: any) {
  copyingIds.value.push(record.id)
  try {
    const res: any = await copyRepository(projectId.value, record.id)
    ElMessage.success(`复制成功，新仓库「${res.data?.name}」`)
    fetchGroups(); fetchList()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '复制失败')
  } finally {
    copyingIds.value = copyingIds.value.filter((id) => id !== record.id)
  }
}

function handleDelete(record: any) {
  ElMessageBox.confirm(
    `确定删除仓库「${record.name}」？删除后将同时删除服务器上已拉取的本地代码目录，且不可恢复。`,
    '确认删除',
    { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
  )
    .then(async () => {
      await deleteRepository(projectId.value, record.id)
      ElMessage.success('删除成功')
      fetchGroups(); fetchList()
    })
    .catch(() => {})
}

// ───────────────────── 展示格式化 ─────────────────────

function formatTime(value: string | null | undefined) {
  return value ? value.substring(0, 19).replace('T', ' ') : '-'
}

function formatCommit(value: string | null | undefined) {
  return value ? value.substring(0, 8) : '-'
}

function formatDuration(ms: number | null | undefined) {
  if (ms == null) return '-'
  if (ms < 1000) return `${ms}ms`
  const seconds = Math.floor(ms / 1000)
  if (seconds < 60) return `${seconds}s`
  const minutes = Math.floor(seconds / 60)
  return `${minutes}m${seconds % 60}s`
}

function formatPullType(value: string) {
  return value === 'CLONE' ? '克隆' : '更新'
}

function statusTagType(status: string | null | undefined) {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'danger'
  if (status === 'RUNNING') return 'warning'
  return 'info'
}

onMounted(() => {
  fetchGroups(); fetchList()
  document.addEventListener('click', onDocClick)
})
onBeforeUnmount(() => {
  document.removeEventListener('click', onDocClick)
})

// ===== 右键菜单关闭 =====
const treeRef = ref()
function onDocClick() { closeContextMenu() }
</script>

<template>
  <div>
    <!-- 页面头部 -->
    <div class="page-header">
      <h2>源代码</h2>
      <el-button v-if="hasPermission('project:repo:add')" type="primary" @click="openCreate">
        + 新建仓库
      </el-button>
    </div>

    <div class="repo-layout">
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
            :draggable="true"
            :allow-drag="allowDrag"
            :allow-drop="allowDrop"
            @node-click="onGroupNodeClick"
            @node-drop="onNodeDrop"
          >
            <template #default="{ data }">
              <div
                :class="['group-tree-node', { active: activeGroupId === data.id }]"
                @contextmenu.stop="handleNodeContextmenu($event, data)"
              >
                <span class="group-name">{{ data.name }}</span>
                <span class="group-count">{{ data.repoCount ?? 0 }}</span>
                <span v-if="data.isSystem === 1" class="group-lock" title="系统默认分组">🔒</span>
              </div>
            </template>
          </el-tree>
        </div>
      </div>

      <!-- 右侧内容 -->
      <div class="repo-content">
        <!-- 项目上下文栏 -->
        <div class="repo-project-bar">
          <span>&#x1F4CC;</span>
          <span>当前项目：<span class="project-name">{{ projectStore.currentProjectName }}</span></span>
          <span class="bar-sep">|</span>
          <span>登记 Git 仓库并拉取代码到服务器，供测试执行使用</span>
        </div>

        <BatchBar
          v-if="hasPermission('project:repo:batch')"
          :selected-count="selectedIds.length"
          :actions="[{ key: 'move', label: '批量修改分组' }, { key: 'delete', label: '批量删除', danger: true }]"
          @action="handleBatchAction"
          @clear="clearSelection"
        />

        <!-- 仓库列表表格 -->
        <div class="repo-table-section">
          <el-table v-loading="loading" :data="list" row-key="id" style="width: 100%" @selection-change="handleSelectionChange">
            <el-table-column type="selection" width="45" />
            <el-table-column prop="name" label="仓库名称" min-width="140">
              <template #default="{ row }">
                <strong>{{ row.name }}</strong>
              </template>
            </el-table-column>
            <el-table-column prop="gitUrl" label="Git 地址" min-width="240" show-overflow-tooltip />
            <el-table-column prop="branch" label="分支" width="100">
              <template #default="{ row }">
                {{ row.branch || '默认' }}
              </template>
            </el-table-column>
            <el-table-column label="分组" width="110">
              <template #default="{ row }">
                {{ groupName(row.groupId) }}
              </template>
            </el-table-column>
            <el-table-column label="认证" width="60" align="center">
              <template #default="{ row }">
                {{ row.hasAuth ? '有' : '无' }}
              </template>
            </el-table-column>
            <el-table-column label="最近拉取状态" width="120" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.lastPullStatus" :type="statusTagType(row.lastPullStatus)" size="small">
                  {{ pullStatusLabel(row.lastPullStatus) }}
                </el-tag>
                <span v-else class="empty-text-inline">-</span>
              </template>
            </el-table-column>
            <el-table-column label="最近拉取时间" width="165">
              <template #default="{ row }">
                {{ formatTime(row.lastPullAt) }}
              </template>
            </el-table-column>
            <el-table-column label="最近 Commit" width="105">
              <template #default="{ row }">
                {{ formatCommit(row.lastCommitId) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="280" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="hasPermission('project:repo:pull')"
                  type="primary"
                  link
                  size="small"
                  :loading="pullingIds.includes(row.id)"
                  @click="handlePull(row)"
                >
                  拉取
                </el-button>
                <el-button
                  v-if="hasPermission('project:repo:logs')"
                  type="primary"
                  link
                  size="small"
                  @click="handleShowLogs(row)"
                >
                  记录
                </el-button>
                <el-button
                  v-if="hasPermission('project:repo:edit')"
                  type="primary"
                  link
                  size="small"
                  @click="openEdit(row)"
                >
                  编辑
                </el-button>
                <el-button
                  v-if="hasPermission('project:repo:add')"
                  type="primary"
                  link
                  size="small"
                  :loading="copyingIds.includes(row.id)"
                  @click="handleCopy(row)"
                >
                  复制
                </el-button>
                <el-button
                  v-if="hasPermission('project:repo:delete')"
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
              <div class="empty-text">暂无数据</div>
            </template>
          </el-table>
        </div>
      </div>
    </div>

    <!-- 新建/编辑仓库弹窗 -->
    <el-dialog
      v-model="modalVisible"
      :title="isEdit ? '编辑仓库' : '新建仓库'"
      width="560px"
      @closed="handleDialogClosed"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="所属分组" prop="groupId">
          <el-select v-model="form.groupId" placeholder="选择所属分组" style="width: 100%">
            <el-option v-for="g in moveTargetGroups" :key="g.id" :value="g.id" :label="g.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="仓库名称" prop="name">
          <el-input v-model="form.name" placeholder="如 auto-test-platform" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="Git 地址" prop="gitUrl">
          <el-input v-model="form.gitUrl" placeholder="https://github.com/user/repo.git" maxlength="500" />
        </el-form-item>
        <el-form-item label="分支" prop="branch">
          <div class="branch-field">
            <el-select
              v-model="form.branch"
              filterable
              allow-create
              default-first-option
              clearable
              :loading="branchLoading"
              placeholder="点击「获取」加载分支，留空使用仓库默认分支"
              style="flex: 1"
            >
              <el-option v-for="item in branchOptions" :key="item" :label="item" :value="item" />
            </el-select>
            <el-button :loading="branchLoading" @click="handleFetchBranches">获取</el-button>
          </div>
          <div v-if="defaultBranch" class="branch-hint">仓库默认分支：{{ defaultBranch }}</div>
        </el-form-item>
        <el-form-item label="认证用户名" prop="authUsername">
          <el-input v-model="form.authUsername" placeholder="私有仓库填写用户名（Token 场景填 Token 用户名）" maxlength="200" />
        </el-form-item>
        <el-form-item label="认证密码 / Token" prop="authPassword">
          <el-input
            v-model="form.authPassword"
            type="password"
            show-password
            :placeholder="isEdit ? '留空保持不变' : '私有仓库填写密码或 Access Token'"
            maxlength="500"
          />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="2"
            placeholder="仓库描述"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="modalVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 拉取记录抽屉 -->
    <el-drawer v-model="logsDrawerVisible" :title="`拉取记录 - ${currentRepoName}`" size="640px">
      <el-table v-loading="logsLoading" :data="logsList" row-key="id" style="width: 100%">
        <el-table-column label="时间" width="165">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="类型" width="70" align="center">
          <template #default="{ row }">
            {{ formatPullType(row.pullType) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">
              {{ pullStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="分支" width="100">
          <template #default="{ row }">
            {{ row.branch || '默认' }}
          </template>
        </el-table-column>
        <el-table-column label="Commit" width="90">
          <template #default="{ row }">
            {{ formatCommit(row.commitId) }}
          </template>
        </el-table-column>
        <el-table-column label="耗时" width="80">
          <template #default="{ row }">
            {{ formatDuration(row.durationMs) }}
          </template>
        </el-table-column>
        <el-table-column label="信息" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.message || '-' }}
          </template>
        </el-table-column>
        <template #empty>
          <div class="empty-text">暂无拉取记录</div>
        </template>
      </el-table>
    </el-drawer>

    <!-- 分组新建/编辑弹窗 -->
    <el-dialog v-model="groupModalVisible" :title="editingGroupId ? '编辑分组' : '新建分组'" width="460px">
      <el-form label-position="top">
        <el-form-item label="分组名称" required>
          <el-input v-model="groupForm.name" placeholder="如：后端服务" />
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
        将选中的 <b style="color: #409eff">{{ selectedIds.length }}</b> 个仓库的分组修改为：
      </p>
      <el-select v-model="batchMoveTarget" placeholder="选择目标分组" filterable style="width: 100%; margin-bottom: 12px">
        <el-option v-for="g in moveTargetGroups" :key="g.id" :value="g.id" :label="g.name" />
      </el-select>
      <div v-if="batchMovePreview.length" class="batch-preview">
        <div class="batch-preview-title">以下仓库的分组将被修改：</div>
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

    <!-- 批量删除确认弹窗 -->
    <el-dialog v-model="batchDeleteVisible" title="批量删除仓库" width="440px">
      <p style="font-size: 14px; color: #606266; line-height: 1.6; margin: 0 0 12px">
        确定删除已选中的 <strong style="color: #f56c6c">{{ selectedIds.length }}</strong> 个仓库吗？<br>
        <span style="color: #909399; font-size: 13px;">删除后将无法恢复，本地代码目录将同步清理。</span>
      </p>
      <div v-if="selectedRows.length" class="batch-preview">
        <div class="batch-preview-title">以下仓库将被删除：</div>
        <div class="batch-preview-list">
          <div v-for="row in selectedRows" :key="row.id" class="batch-preview-item">
            <span class="batch-preview-name">{{ row.name }}</span>
            <span class="batch-preview-old">{{ row.gitUrl }}</span>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="batchDeleteVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmBatchDelete">确认删除</el-button>
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
          <div v-if="hasPermission('project:repo:group')" class="context-menu-item" @click="contextCreateGroup">新建分组</div>
        </template>
        <!-- 用户分组右键 -->
        <template v-else>
          <div v-if="hasPermission('project:repo:group')" class="context-menu-item" @click="contextCreateChild">新建子分组</div>
          <div v-if="hasPermission('project:repo:group')" class="context-menu-divider" />
          <div class="context-menu-item" @click="contextEdit">编辑</div>
          <div class="context-menu-item danger" @click="contextDelete">删除</div>
        </template>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
}

.repo-project-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  background: #ecf5ff;
  border: 1px solid #c6e2ff;
  border-radius: 6px;
  margin-bottom: 16px;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.65);
}

.repo-project-bar .project-name {
  font-weight: 600;
  color: #409eff;
}

.repo-project-bar .bar-sep {
  color: rgba(0, 0, 0, 0.25);
}

.repo-layout {
  display: flex;
  gap: 16px;
  min-height: calc(100vh - 214px);
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

.repo-content {
  flex: 1;
  min-width: 0;
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

.repo-table-section {
  background: #fff;
  border-radius: 8px;
  border: 1px solid #f0f0f0;
}

.empty-text {
  padding: 32px 0;
  color: rgba(0, 0, 0, 0.25);
  font-size: 13px;
}

.empty-text-inline {
  color: rgba(0, 0, 0, 0.25);
}

.branch-field {
  display: flex;
  gap: 8px;
  width: 100%;
}

.branch-hint {
  width: 100%;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  line-height: 1.4;
}
</style>
