<!--
 @author HXN
 @date 2026-08-20 15:34
 @description 测试计划列表视图（含分组树 + 高级搜索 + 执行统计）
-->
<script setup lang="ts">
/**
 * 测试计划列表 - M9
 * 左侧分组树 + 右侧搜索/表格（含触发方式、用例数、最近执行、通过率列）
 */
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPlans, deletePlan, getPlanGroups, createPlanGroup, updatePlanGroup, deletePlanGroup } from '@/api/plan'
import { startExecution } from '@/api/execution'
import { getEnvironments } from '@/api/environment'
import { usePermission } from '@/composables/usePermission'

const route = useRoute()
const router = useRouter()
const { hasPermission } = usePermission()
const projectId = computed(() => Number(route.params.id))

// ===== 分组树 =====
const groupLoading = ref(false)
const groups = ref<any[]>([])
const activeGroupId = ref<string | number>('all')
const groupSearchKey = ref('')
const expandedIds = ref<Set<number>>(new Set())

interface GroupNode {
  id: string | number
  name: string
  description?: string
  parentId: number | null
  planCount: number
  isSystem: boolean
}

const systemGroups: GroupNode[] = [
  { id: 'all', name: '全部', parentId: null, planCount: 0, isSystem: true },
  { id: 'ungrouped', name: '未分组', parentId: null, planCount: 0, isSystem: true },
]

const allGroups = computed<GroupNode[]>(() => {
  const userGroups: GroupNode[] = groups.value.map((g: any) => ({
    id: g.id,
    name: g.name,
    description: g.description,
    parentId: g.parentId,
    planCount: g.planCount || 0,
    isSystem: false,
  }))
  return [...systemGroups, ...userGroups]
})

const filteredUserGroups = computed(() => {
  const key = groupSearchKey.value.toLowerCase()
  if (!key) return allGroups.value.filter(g => !g.isSystem)
  return allGroups.value.filter(g => !g.isSystem && g.name.toLowerCase().includes(key))
})

const topLevelUserGroups = computed(() =>
  filteredUserGroups.value.filter(g => g.parentId === null)
)

function getChildren(parentId: number): GroupNode[] {
  return filteredUserGroups.value.filter(g => g.parentId === parentId)
}

function selectGroup(id: string | number) {
  activeGroupId.value = id
  pagination.current = 1
  fetchList()
}

function toggleExpand(id: number) {
  if (expandedIds.value.has(id)) {
    expandedIds.value.delete(id)
  } else {
    expandedIds.value.add(id)
  }
}

async function fetchGroups() {
  groupLoading.value = true
  try {
    const res: any = await getPlanGroups(projectId.value)
    groups.value = res.data || []
    // 更新系统分组的计数
    const allCount = list.value.length > 0 ? pagination.total : 0
    systemGroups[0].planCount = allCount
    systemGroups[1].planCount = groups.value.length > 0
      ? Math.max(0, allCount - groups.value.reduce((sum: number, g: any) => sum + (g.planCount || 0), 0))
      : 0
  } catch { groups.value = [] } finally { groupLoading.value = false }
}

// ===== 右键菜单 =====
const contextMenuVisible = ref(false)
const contextMenuPos = reactive({ x: 0, y: 0 })
const contextGroup = ref<GroupNode | null>(null)
const isBlankContext = ref(false)

function showBlankContextMenu(e: MouseEvent) {
  const target = e.target as HTMLElement
  if (target.closest('.group-tree-item')) return
  e.preventDefault()
  isBlankContext.value = true
  contextGroup.value = null
  contextMenuPos.x = e.clientX
  contextMenuPos.y = e.clientY
  contextMenuVisible.value = true
}

function showGroupContextMenu(e: MouseEvent, group: GroupNode) {
  if (group.isSystem) return
  e.preventDefault()
  e.stopPropagation()
  isBlankContext.value = false
  contextGroup.value = group
  contextMenuPos.x = e.clientX
  contextMenuPos.y = e.clientY
  contextMenuVisible.value = true
}

function hideContextMenu() {
  contextMenuVisible.value = false
}

// ===== 分组弹窗 =====
const groupDialogVisible = ref(false)
const groupDialogTitle = ref('新建分组')
const groupForm = reactive({ name: '', description: '', parentId: null as number | null })
const editingGroupId = ref<number | null>(null)

function openCreateGroup(parentId: number | null = null) {
  editingGroupId.value = null
  groupDialogTitle.value = parentId ? '新建子分组' : '新建分组'
  groupForm.name = ''
  groupForm.description = ''
  groupForm.parentId = parentId
  groupDialogVisible.value = true
  hideContextMenu()
}

function openEditGroup(group: GroupNode) {
  editingGroupId.value = group.id as number
  groupDialogTitle.value = '编辑分组'
  groupForm.name = group.name
  groupForm.description = group.description || ''
  groupForm.parentId = group.parentId
  groupDialogVisible.value = true
  hideContextMenu()
}

async function handleGroupSubmit() {
  if (!groupForm.name.trim()) {
    ElMessage.warning('请输入分组名称')
    return
  }
  try {
    if (editingGroupId.value) {
      await updatePlanGroup(editingGroupId.value, {
        name: groupForm.name,
        description: groupForm.description || undefined,
        parentId: groupForm.parentId,
      })
      ElMessage.success('保存成功')
    } else {
      await createPlanGroup(projectId.value, {
        name: groupForm.name,
        description: groupForm.description || undefined,
        parentId: groupForm.parentId,
      })
      ElMessage.success('创建成功')
    }
    groupDialogVisible.value = false
    fetchGroups()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  }
}

async function handleDeleteGroup(group: GroupNode) {
  const childCount = filteredUserGroups.value.filter(g => g.parentId === group.id).length
  const hint = childCount > 0
    ? `删除后，子分组将一并删除，所有计划将自动归入「未分组」。`
    : `删除后，该分组下的计划将自动归入「未分组」。`
  try {
    await ElMessageBox.confirm(
      `确定删除分组「${group.name}」吗？${hint}`,
      '确认删除',
      { type: 'warning' }
    )
    await deletePlanGroup(group.id as number)
    ElMessage.success('删除成功')
    if (activeGroupId.value === group.id) {
      activeGroupId.value = 'all'
    }
    fetchGroups()
    fetchList()
  } catch {}
  hideContextMenu()
}

// ===== 搜索 =====
const searchCollapsed = ref(true)
const searchForm = reactive({
  name: '',
  triggerType: '',
  environmentId: null as number | null,
  status: '' as string,
  suiteKeyword: '',
  updateBegin: '',
  updateEnd: '',
})
const environments = ref<any[]>([])

async function loadEnvironments() {
  try {
    const res: any = await getEnvironments(projectId.value)
    environments.value = res.data || []
  } catch { environments.value = [] }
}

function handleSearch() {
  pagination.current = 1
  fetchList()
}

function handleReset() {
  searchForm.name = ''
  searchForm.triggerType = ''
  searchForm.environmentId = null
  searchForm.status = ''
  searchForm.suiteKeyword = ''
  searchForm.updateBegin = ''
  searchForm.updateEnd = ''
  handleSearch()
}

// ===== 列表 =====
const loading = ref(false)
const list = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 20, total: 0 })

async function fetchList() {
  loading.value = true
  try {
    const params: any = {
      page: pagination.current,
      pageSize: pagination.pageSize,
    }
    // 搜索条件拼入 keyword（后端支持 name/description 模糊匹配）
    if (searchForm.name) params.keyword = searchForm.name

    // 分组过滤
    if (activeGroupId.value === 'ungrouped') {
      params.groupId = 0
    } else if (activeGroupId.value !== 'all') {
      params.groupId = activeGroupId.value
    }

    const res: any = await getPlans(projectId.value, params)
    list.value = res.data?.items || []
    pagination.total = res.data?.total || 0

    // 刷新分组计数
    fetchGroups()
  } catch { list.value = [] } finally { loading.value = false }
}

// ===== 操作 =====
function handleEdit(record: any) {
  router.push(`/project/${projectId}/plans/${record.id}/edit`)
}

function handleDelete(record: any) {
  ElMessageBox.confirm(`确定删除计划「${record.name}」？`, '确认删除', { type: 'warning' })
    .then(async () => {
      await deletePlan(record.id)
      ElMessage.success('删除成功')
      fetchList()
    })
    .catch(() => {})
}

async function handleRun(record: any) {
  ElMessageBox.confirm(`确定执行计划「${record.name}」？`, '触发执行', { type: 'info' })
    .then(async () => {
      try {
        const res: any = await startExecution(record.id)
        ElMessage.success('执行已触发')
        router.push(`/project/${projectId}/executions/${res.data.id}`)
      } catch { ElMessage.error('触发失败') }
    })
    .catch(() => {})
}

// ===== 辅助 =====
const triggerTypeLabels: Record<string, string> = {
  MANUAL: '手动',
  SCHEDULED: '定时',
  CI: 'CI',
}

const triggerTypeTagMap: Record<string, string> = {
  MANUAL: '',
  SCHEDULED: 'success',
  CI: 'warning',
}

function formatDateTime(dt: string | null) {
  if (!dt) return '-'
  return dt.substring(0, 16).replace('T', ' ')
}

function getPassRateColor(rate: number | null) {
  if (rate == null) return ''
  if (rate >= 90) return '#67c23a'
  if (rate >= 70) return '#e6a23c'
  return '#f56c6c'
}

// ===== 前端补充过滤（后端 keyword 仅支持 name/description，其他条件前端过滤） =====
const filteredList = computed(() => {
  return list.value.filter((row: any) => {
    if (searchForm.triggerType && row.triggerType !== searchForm.triggerType) return false
    if (searchForm.environmentId && row.environmentId !== searchForm.environmentId) return false
    if (searchForm.status) {
      const expected = searchForm.status === '1' ? 1 : 0
      if (row.isActive !== expected) return false
    }
    if (searchForm.suiteKeyword) {
      const kw = searchForm.suiteKeyword.toLowerCase()
      const names = (row.suiteNames || []) as string[]
      if (!names.some(n => n.toLowerCase().includes(kw))) return false
    }
    return true
  })
})

// ===== 初始化 =====
onMounted(() => {
  loadEnvironments()
  fetchList()
})
</script>

<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <h2 style="margin:0">测试计划</h2>
      <el-button v-if="hasPermission('project:plan:add')" type="primary"
        @click="router.push(`/project/${projectId}/plans/new`)">+ 新建计划</el-button>
    </div>

    <div class="plan-layout">
      <!-- 左侧分组树 -->
      <div class="group-panel" @contextmenu="showBlankContextMenu">
        <div class="group-search">
          <el-input v-model="groupSearchKey" placeholder="搜索分组..." size="small" clearable />
        </div>

        <!-- 系统分组 -->
        <div v-for="sg in systemGroups" :key="sg.id"
          class="group-tree-item"
          :class="{ active: activeGroupId === sg.id }"
          @click="selectGroup(sg.id)">
          <span class="group-icon">{{ sg.id === 'all' ? '📂' : '📁' }}</span>
          <span class="group-name">{{ sg.name }}</span>
          <span class="group-lock" title="系统默认分组">🔒</span>
          <span class="group-count">{{ sg.planCount }}</span>
        </div>

        <div class="group-divider" />

        <!-- 用户分组 -->
        <template v-for="group in topLevelUserGroups" :key="group.id">
          <div class="group-tree-item"
            :class="{ active: activeGroupId === group.id }"
            @click="selectGroup(group.id)"
            @contextmenu="showGroupContextMenu($event, group)">
            <span v-if="getChildren(group.id as number).length > 0"
              class="group-arrow"
              :class="{ expanded: expandedIds.has(group.id as number) }"
              @click.stop="toggleExpand(group.id as number)">&#9654;</span>
            <span v-else class="group-arrow-placeholder" />
            <span class="group-icon">📁</span>
            <span class="group-name" :title="group.name">{{ group.name }}</span>
            <span class="group-count">{{ group.planCount }}</span>
          </div>
          <!-- 子分组（递归一层，深层可后续扩展） -->
          <template v-if="expandedIds.has(group.id as number)">
            <div v-for="child in getChildren(group.id as number)" :key="child.id"
              class="group-tree-item child"
              :class="{ active: activeGroupId === child.id }"
              @click="selectGroup(child.id)"
              @contextmenu="showGroupContextMenu($event, child)">
              <span class="group-arrow-placeholder" />
              <span class="group-icon">📁</span>
              <span class="group-name" :title="child.name">{{ child.name }}</span>
              <span class="group-count">{{ child.planCount }}</span>
            </div>
          </template>
        </template>
      </div>

      <!-- 右侧内容 -->
      <div class="plan-content">
        <!-- 搜索卡片 -->
        <div class="search-card" :class="{ collapsed: searchCollapsed }">
          <div class="search-form">
            <div class="search-row">
              <div class="search-item">
                <label class="filter-label">计划名称</label>
                <el-input v-model="searchForm.name" placeholder="模糊查询" size="default" clearable @keyup.enter="handleSearch" />
              </div>
              <div class="search-item">
                <label class="filter-label">触发方式</label>
                <el-select v-model="searchForm.triggerType" placeholder="全部触发方式" clearable size="default">
                  <el-option label="定时执行" value="SCHEDULED" />
                  <el-option label="手动触发" value="MANUAL" />
                  <el-option label="CI 触发" value="CI" />
                </el-select>
              </div>
              <div class="search-item">
                <label class="filter-label">环境</label>
                <el-select v-model="searchForm.environmentId" placeholder="全部环境" clearable size="default">
                  <el-option v-for="env in environments" :key="env.id" :value="env.id" :label="env.name" />
                </el-select>
              </div>
              <div class="search-actions">
                <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
                <el-button size="default" @click="handleReset">重置</el-button>
                <button class="search-toggle" type="button" @click="searchCollapsed = !searchCollapsed">
                  <span class="arrow" :class="{ up: !searchCollapsed }">&#708;</span>
                  <span class="text">{{ searchCollapsed ? '展开' : '收起' }}</span>
                </button>
              </div>
            </div>
            <div v-show="!searchCollapsed" class="search-row collapse-row">
              <div class="search-item">
                <label class="filter-label">状态</label>
                <el-select v-model="searchForm.status" placeholder="全部状态" clearable size="default">
                  <el-option label="启用" value="1" />
                  <el-option label="禁用" value="0" />
                </el-select>
              </div>
              <div class="search-item">
                <label class="filter-label">关联套件</label>
                <el-input v-model="searchForm.suiteKeyword" placeholder="套件名称" size="default" clearable />
              </div>
              <div class="search-item">
                <label class="filter-label">最近更新</label>
                <div style="display:flex;gap:4px;align-items:center;flex:1">
                  <el-date-picker v-model="searchForm.updateBegin" type="date" placeholder="起" size="default" style="flex:1" value-format="YYYY-MM-DD" />
                  <span style="color:#999">至</span>
                  <el-date-picker v-model="searchForm.updateEnd" type="date" placeholder="止" size="default" style="flex:1" value-format="YYYY-MM-DD" />
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 表格 -->
        <el-table v-loading="loading" :data="filteredList" row-key="id" border style="width:100%">
          <el-table-column prop="name" label="计划名称" min-width="160" show-overflow-tooltip>
            <template #default="{ row }">
              <a style="font-weight:500;color:var(--color-primary);cursor:pointer" @click="handleEdit(row)">{{ row.name }}</a>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" min-width="160" show-overflow-tooltip />
          <el-table-column label="触发方式" width="90" align="center">
            <template #default="{ row }">
              <el-tag :type="(triggerTypeTagMap[row.triggerType] || '') as any" size="small">
                {{ triggerTypeLabels[row.triggerType] || row.triggerType }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="关联套件" min-width="160">
            <template #default="{ row }">
              <template v-if="row.suiteNames && row.suiteNames.length > 0">
                <el-tag v-for="(name, idx) in row.suiteNames" :key="idx" size="small" type="info"
                  style="margin:2px 4px 2px 0">{{ name }}</el-tag>
              </template>
              <span v-else style="color:#999">-</span>
            </template>
          </el-table-column>
          <el-table-column label="用例数" width="70" align="center">
            <template #default="{ row }">{{ row.caseCount || 0 }}</template>
          </el-table-column>
          <el-table-column prop="environmentName" label="环境" width="100" show-overflow-tooltip />
          <el-table-column label="状态" width="70" align="center">
            <template #default="{ row }">
              <el-tag :type="row.isActive === 1 ? 'success' : 'info'" size="small">
                {{ row.isActive === 1 ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="最近执行" width="150">
            <template #default="{ row }">
              <div v-if="row.lastExecutionTime" style="font-size:12px;color:#606266">
                {{ formatDateTime(row.lastExecutionTime) }}
              </div>
              <div v-if="row.scheduleCron" style="font-size:11px;color:#909399;font-family:monospace;margin-top:2px">
                {{ row.scheduleCron }}
              </div>
              <span v-if="!row.lastExecutionTime" style="color:#c0c4cc">-</span>
            </template>
          </el-table-column>
          <el-table-column label="通过率" width="80" align="center">
            <template #default="{ row }">
              <b v-if="row.passRate != null" :style="{ color: getPassRateColor(row.passRate) }">
                {{ row.passRate }}%
              </b>
              <span v-else style="color:#c0c4cc">-</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right" align="center">
            <template #default="{ row }">
              <el-button v-if="hasPermission('project:plan:edit')" type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button v-if="hasPermission('project:plan:run')" type="success" link size="small" @click="handleRun(row)">执行</el-button>
              <el-button v-if="hasPermission('project:plan:delete')" type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div v-if="pagination.total > 0" class="pagination">
          <el-pagination
            v-model:current-page="pagination.current"
            v-model:page-size="pagination.pageSize"
            :total="pagination.total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            background
            @current-change="(p: number) => { pagination.current = p; fetchList() }"
            @size-change="(s: number) => { pagination.pageSize = s; pagination.current = 1; fetchList() }"
          />
        </div>
      </div>
    </div>

    <!-- 右键菜单 -->
    <teleport to="body">
      <div v-if="contextMenuVisible" class="context-menu"
        :style="{ left: contextMenuPos.x + 'px', top: contextMenuPos.y + 'px' }"
        @click="hideContextMenu">
        <template v-if="isBlankContext">
          <div class="context-menu-item" @click="openCreateGroup()">新建分组</div>
        </template>
        <template v-else>
          <div class="context-menu-item" @click="openCreateGroup(contextGroup?.id as number)">新建子分组</div>
          <div class="context-menu-divider" />
          <div class="context-menu-item" @click="openEditGroup(contextGroup!)">编辑</div>
          <div class="context-menu-item danger" @click="handleDeleteGroup(contextGroup!)">删除</div>
        </template>
      </div>
      <!-- 点击空白关闭菜单 -->
      <div v-if="contextMenuVisible" class="context-menu-mask" @click="hideContextMenu" @contextmenu.prevent="hideContextMenu" />
    </teleport>

    <!-- 新建/编辑分组弹窗 -->
    <el-dialog v-model="groupDialogVisible" :title="groupDialogTitle" width="420px" :close-on-click-modal="false">
      <el-form label-position="top">
        <el-form-item label="分组名称" required>
          <el-input v-model="groupForm.name" placeholder="请输入分组名称，如「日常测试」" maxlength="100" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="groupForm.description" placeholder="可选，分组描述" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="groupDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleGroupSubmit">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.plan-layout {
  display: flex;
  background: #fff;
  border-radius: 4px;
  border: 1px solid #f0f0f0;
  overflow: hidden;
  min-height: calc(100vh - 200px);
}

/* ===== 分组面板 ===== */
.group-panel {
  width: 260px;
  border-right: 1px solid #f0f0f0;
  padding: 12px 0;
  flex-shrink: 0;
  overflow-y: auto;
}
.group-search {
  padding: 0 12px;
  margin-bottom: 8px;
}
.group-divider {
  height: 1px;
  background: #f0f0f0;
  margin: 8px 12px;
}
.group-tree-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px 6px 16px;
  cursor: pointer;
  font-size: 14px;
  color: #606266;
  transition: all .15s;
}
.group-tree-item:hover {
  background: #fafafa;
}
.group-tree-item.active {
  background: #ecf5ff;
  color: #409eff;
}
.group-tree-item.child {
  padding-left: 36px;
}
.group-icon {
  font-size: 14px;
  flex-shrink: 0;
}
.group-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.group-lock {
  font-size: 11px;
  color: #c0c4cc;
  margin-left: 4px;
  flex-shrink: 0;
}
.group-count {
  margin-left: auto;
  font-size: 12px;
  background: #f5f5f5;
  color: #909399;
  padding: 0 6px;
  border-radius: 10px;
  min-width: 24px;
  text-align: center;
  line-height: 20px;
  flex-shrink: 0;
}
.group-arrow {
  display: inline-block;
  width: 14px;
  font-size: 9px;
  color: #c0c4cc;
  cursor: pointer;
  flex-shrink: 0;
  text-align: center;
  transition: transform .15s;
}
.group-arrow.expanded {
  transform: rotate(90deg);
}
.group-arrow-placeholder {
  display: inline-block;
  width: 14px;
  flex-shrink: 0;
}

/* ===== 右侧内容 ===== */
.plan-content {
  flex: 1;
  padding: 16px 20px;
  overflow: auto;
}

/* ===== 搜索卡片 ===== */
.search-card {
  background: #fff;
  border-radius: 4px;
  border: 1px solid #f0f0f0;
  padding: 16px 16px 12px;
  margin-bottom: 16px;
}
.search-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.search-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr) auto;
  gap: 12px;
  align-items: center;
}
.search-item {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.search-item .filter-label {
  min-width: 72px;
  max-width: 72px;
  flex-shrink: 0;
  font-size: 13px;
  color: #606266;
}
.search-item .el-input,
.search-item .el-select {
  flex: 1;
  min-width: 0;
}
.search-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.search-toggle {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 32px;
  padding: 0 8px;
  border: none;
  background: none;
  color: #409eff;
  font-size: 13px;
  cursor: pointer;
}
.search-toggle:hover {
  color: #66b1ff;
}
.search-toggle .arrow {
  display: inline-block;
  transition: transform .2s;
  font-size: 11px;
}
.search-toggle .arrow.up {
  transform: rotate(180deg);
}

/* ===== 右键菜单 ===== */
.context-menu {
  position: fixed;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  box-shadow: 0 2px 12px rgba(0,0,0,.1);
  padding: 4px 0;
  min-width: 120px;
  z-index: 3000;
}
.context-menu-item {
  padding: 6px 12px;
  font-size: 13px;
  color: #303133;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
}
.context-menu-item:hover {
  background: #f5f7fa;
}
.context-menu-item.danger {
  color: #f56c6c;
}
.context-menu-divider {
  height: 1px;
  background: #e4e7ed;
  margin: 4px 0;
}
.context-menu-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 2999;
}

/* ===== 分页 ===== */
.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
